package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.components.SourcePosition
import org.orbit.core.getPath
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.types.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.toPath

abstract class Entity(
    override val name: String,
    open val properties: List<Property> = emptyList(),
    open val traitConformance: List<Trait> = emptyList(),
    override val equalitySemantics: Equality<out Entity, out Entity>
) : ValuePositionType, TypeExpression {
    constructor(path: Path, properties: List<Property> = emptyList(), traitConformance: List<Trait> = emptyList(), equalitySemantics: Equality<out Entity, out Entity>)
        : this(path.toString(OrbitMangler), properties, traitConformance, equalitySemantics)

    override fun evaluate(context: Context): TypeProtocol = this

    override fun equals(other: Any?): Boolean = when (other) {
        is Entity -> name == other.name
        else -> false
    }
}

interface VirtualType : TypeProtocol

data class TypeProjection(val type: Type, val trait: Trait) : VirtualType {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = type.equalitySemantics
    override val name: String = type.name
}

data class TypeParameter(override val name: String, val constraints: List<Trait> = emptyList()) : VirtualType {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = NominalEquality

    constructor(path: Path, constraints: List<Trait> = emptyList()) : this(path.toString(OrbitMangler), constraints)
    // TODO - Complex Type Parameter Expressions
    constructor(node: TypeIdentifierNode) : this(node.getPath())
}

data class Type(override val name: String, val typeParameters: List<ValuePositionType> = emptyList(), override val properties: List<Property> = emptyList(),
                override val traitConformance: List<Trait> = emptyList(), override val equalitySemantics: Equality<Entity, Entity> = NominalEquality, val isRequired: Boolean = false) : Entity(name, properties, traitConformance, equalitySemantics) {
    constructor(path: Path, typeParameters: List<ValuePositionType> = emptyList(), properties: List<Property> = emptyList(), traitConformance: List<Trait> = emptyList(), equalitySemantics: Equality<Entity, Entity> = NominalEquality, isRequired: Boolean = false)
        : this(path.toString(OrbitMangler), typeParameters, properties, traitConformance, equalitySemantics, isRequired)

    constructor(node: TypeDefNode)
        : this(node.getPath())
}

data class Trait(override val name: String, val typeParameters: List<ValuePositionType> = emptyList(), override val properties: List<Property> = emptyList(), override val traitConformance: List<Trait> = emptyList(), val signatures: List<SignatureProtocol<*>> = emptyList(), override val equalitySemantics: Equality<Trait, Type> = TraitConformanceEquality, val implicit: Boolean = false) : Entity(name, properties, traitConformance, equalitySemantics) {
    constructor(path: Path, typeParameters: List<ValuePositionType> = emptyList(), properties: List<Property> = emptyList(), traitConformance: List<Trait> = emptyList(), signatures: List<SignatureProtocol<*>> = emptyList(), equalitySemantics: Equality<Trait, Type> = StructuralEquality, implicit: Boolean = false)
        : this(path.toString(OrbitMangler), typeParameters, properties, traitConformance, signatures, equalitySemantics, implicit)

    constructor(node: TraitDefNode) : this(node.getPath())

    override fun equals(other: Any?): Boolean = when (other) {
        is Trait -> name == other.name
        else -> false
    }
}

data class SynthesisedEntity(val entity: Entity, val metaType: MetaType): TypeProtocol by entity

data class TypeAlias(override val name: String, val targetType: Type) : VirtualType, TypeExpression {
    constructor(path: Path, targetType: Type) : this(path.toString(OrbitMangler), targetType)

    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol>
        get() = targetType.equalitySemantics

    override fun evaluate(context: Context): TypeProtocol = targetType

    override fun toString(printer: Printer): String {
        return "(${printer.apply(name, PrintableKey.Italics)} = ${targetType.toString(printer)})"
    }
}

interface EntityConstructor : TypeProtocol {
    val typeParameters: List<TypeParameter>
    val properties: List<Property>
}

data class TypeConstructor(override val name: String, override val typeParameters: List<TypeParameter>, override val properties: List<Property> = emptyList()) : EntityConstructor {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = TypeConstructorEquality

    constructor(path: Path, typeParameters: List<TypeParameter> = emptyList(), properties: List<Property> = emptyList()) : this(path.toString(OrbitMangler), typeParameters, properties)
    constructor(node: TypeConstructorNode, typeParameters: List<TypeParameter> = emptyList(), properties: List<Property> = emptyList()) : this(node.getPath(), typeParameters, properties)

    fun getTypeParameterOrNull(path: Path) : TypeParameter? {
        return typeParameters.find { OrbitMangler.unmangle(it.name) == path }
    }
}

data class TraitConstructor(override val name: String, override val typeParameters: List<TypeParameter>, override val properties: List<Property> = emptyList()) : EntityConstructor {
    // TODO - We need a separate 'TraitConstructorEquality' because of method signatures in trait constructors
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = TypeConstructorEquality

    constructor(path: Path, typeParameters: List<TypeParameter> = emptyList(), properties: List<Property> = emptyList()) : this(path.toString(OrbitMangler), typeParameters, properties)
}

data class MetaType(val entityConstructor: EntityConstructor, val concreteTypeParameters: List<ValuePositionType>, val properties: List<Property>) : ValuePositionType, TypeExpression {
    companion object : KoinComponent {
        val invocation: Invocation by inject()
    }

    override val name: String
        get() = entityConstructor.name

    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol>
        get() = entityConstructor.equalitySemantics

    override fun evaluate(context: Context): TypeProtocol {
        // TODO - Verify concrete types satisfy typeConstructor's type parameters
        val typeParams = concreteTypeParameters.toMutableList()
        val cCount = concreteTypeParameters.count()
        val eCount = entityConstructor.typeParameters.count()

        if (cCount > eCount) {
            throw invocation.make<TypeSystem>("Type constructor ${entityConstructor.name} expects ${entityConstructor.typeParameters.count()} type parameters, found ${concreteTypeParameters.count()}", SourcePosition.unknown)
        } else if (eCount > cCount) {
            val diff = eCount - cCount

            for (i in IntRange(0, diff - 1)) {
                typeParams.add(IntrinsicTypes.AnyType.type)
            }

            val typeParamsString = typeParams.joinToString(", ") { it.name }

            // TODO - is this a good idea? Do we need an explicit wildcard?
            invocation.warn("Type constructor invocation found without explicit type parameters. Type is inferred to be ${entityConstructor.name}<$typeParamsString>", SourcePosition.unknown)
        }

        val paramsPath = Path(entityConstructor.name) + typeParams.map { Path(it.name) }

        return when (entityConstructor) {
            is TypeConstructor -> Type(paramsPath, concreteTypeParameters, properties)
            is TraitConstructor -> Trait(paramsPath, concreteTypeParameters, properties)
            else -> TODO("???")
        }
    }
}

data class Parameter(override val name: String, val type: TypeProtocol) : TypeProtocol {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = type.equalitySemantics

    override fun toString(printer: Printer): String {
        return "${name}: ${type.toString(printer)}"
    }
}

interface SignatureProtocol<T: TypeProtocol> : ValuePositionType {
    val receiver: T
    val parameters: List<Parameter>
    val returnType: ValuePositionType

    fun toString(mangler: Mangler) : String
    fun isReceiverSatisfied(by: Entity, context: Context) : Boolean
    fun isReturnTypeSatisfied(by: Entity, context: Context) : Boolean {
        return (returnType.equalitySemantics as AnyEquality).isSatisfied(context, returnType, by)
    }

    fun isParameterListSatisfied(by: List<Parameter>, context: Context) : Boolean {
        return parameters.count() == by.count() && parameters.zip(by).all {
            (it.first.equalitySemantics as AnyEquality).isSatisfied(context, it.first, it.second)
        }
    }

    fun isSatisfied(by: SignatureProtocol<*>, context: Context)
        = isReceiverSatisfied(by.receiver as Entity, context)
            && isReturnTypeSatisfied(by.returnType as Entity, context)
            && isParameterListSatisfied(by.parameters, context)

    override fun toString(printer: Printer): String {
        val params = parameters.joinToString(", ") { it.toString(printer) }

        return """
            (${receiver.toString(printer)}) ${printer.apply(name, PrintableKey.Italics)} ($params) (${returnType.toString(printer)})
        """.trimIndent()
    }
}

data class InstanceSignature(
    override val name: String,
    override val receiver: Parameter,
    override val parameters: List<Parameter>, override val returnType: ValuePositionType
) : SignatureProtocol<Parameter> {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = SignatureEquality

    override fun toString(mangler: Mangler): String {
        return mangler.mangle(this)
    }

    override fun isReceiverSatisfied(by: Entity, context: Context): Boolean {
        return (receiver.type.equalitySemantics as AnyEquality).isSatisfied(context, receiver.type, by)
    }
}

data class TypeSignature(
    override val name: String,
    override val receiver: ValuePositionType,
    override val parameters: List<Parameter>,
    override val returnType: ValuePositionType
) : SignatureProtocol<ValuePositionType> {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = SignatureEquality

    override fun toString(mangler: Mangler): String {
        return mangler.mangle(this)
    }

    override fun isReceiverSatisfied(by: Entity, context: Context): Boolean {
        return (receiver.equalitySemantics as AnyEquality).isSatisfied(context, receiver, by)
    }
}

enum class OperatorPosition {
    Prefix, Infix, Postfix;
}

interface IntrinsicOperators {
    val position: OperatorPosition
    val returnType: TypeProtocol

    fun getType() : Operator
    fun getPath() : Path
}

class IntOperators {
    companion object {
        fun all() : Set<TypeProtocol> {
            return (Prefix.values().map(IntrinsicOperators::getType)
                + Infix.values().map(IntrinsicOperators::getType)).toSet()
        }
    }

    enum class Prefix(override val returnType: TypeProtocol, val symbol: String) : IntrinsicOperators {
        Plus(IntrinsicTypes.Int.type, "+"),
        Negation(IntrinsicTypes.Int.type, "-");

        override val position: OperatorPosition = OperatorPosition.Prefix

        override fun getType(): Operator {
            return PrefixOperator(symbol, IntrinsicTypes.Int.type, returnType)
        }

        override fun getPath(): Path {
            return IntrinsicTypes.Int.path + symbol + returnType.name.toPath()
        }
    }

    enum class Infix(private val leftType: TypeProtocol, private val rightType: TypeProtocol, override val returnType: TypeProtocol, val symbol: String) :
        IntrinsicOperators {
        Addition(IntrinsicTypes.Int.type, IntrinsicTypes.Int.type, IntrinsicTypes.Int.type, "+"),
        Subtraction(IntrinsicTypes.Int.type, IntrinsicTypes.Int.type, IntrinsicTypes.Int.type, "-"),
        Multiplication(IntrinsicTypes.Int.type, IntrinsicTypes.Int.type, IntrinsicTypes.Int.type, "*");
        // TODO - Division, Exponentiation, Modulo etc

        override val position: OperatorPosition = OperatorPosition.Infix

        override fun getType(): Operator {
            return InfixOperator(symbol, leftType, rightType, returnType)
        }

        override fun getPath(): Path {
            return IntrinsicTypes.Int.path + symbol + leftType.name.toPath() + rightType.name.toPath()
        }
    }
}

