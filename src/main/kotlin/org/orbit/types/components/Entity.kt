package org.orbit.types.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.types.phase.TypeChecker
import org.orbit.util.Invocation
import org.orbit.util.pluralise
import org.orbit.util.toPath

abstract class Entity(
    override val name: String,
    open val properties: List<Property> = emptyList(),
    override val equalitySemantics: Equality<Entity>
) : ValuePositionType, TypeExpression {
    constructor(path: Path, properties: List<Property> = emptyList(), equalitySemantics: Equality<Entity>)
        : this(path.toString(OrbitMangler), properties, equalitySemantics)

    override fun evaluate(context: Context): TypeProtocol = this

    override fun equals(other: Any?): Boolean = when (other) {
        is Entity -> name == other.name
        else -> false
    }
}

data class TypeParameter(override val name: String) : TypeProtocol {
    override val equalitySemantics: Equality<out TypeProtocol> = NominalEquality

    constructor(path: Path) : this(path.toString(OrbitMangler))
}

data class Type(override val name: String, val typeParameters: List<ValuePositionType> = emptyList(), override val properties: List<Property> = emptyList(), override val equalitySemantics: Equality<Entity> = NominalEquality, val isRequired: Boolean = false) : Entity(name, properties, equalitySemantics) {
    constructor(path: Path, typeParameters: List<ValuePositionType> = emptyList(), properties: List<Property> = emptyList(), equalitySemantics: Equality<Entity> = NominalEquality, isRequired: Boolean = false)
        : this(path.toString(OrbitMangler), typeParameters, properties, equalitySemantics, isRequired)
}

data class Trait(override val name: String, override val properties: List<Property> = emptyList(), val signatures: List<SignatureProtocol<*>>, override val equalitySemantics: Equality<Entity> = StructuralEquality) : Entity(name, properties,equalitySemantics) {
    constructor(path: Path, properties: List<Property> = emptyList(), signatures: List<SignatureProtocol<*>> = emptyList(), equalitySemantics: Equality<Entity> = StructuralEquality)
        : this(path.toString(OrbitMangler), properties, signatures, equalitySemantics)
}

data class TypeAlias(override val name: String, val targetType: Type) : TypeProtocol, TypeExpression {
    override val equalitySemantics: Equality<out TypeProtocol>
        get() = targetType.equalitySemantics

    override fun evaluate(context: Context): TypeProtocol = targetType
}

data class TypeConstructor(override val name: String, val typeParameters: List<TypeParameter>) : TypeProtocol {
    override val equalitySemantics: Equality<out TypeProtocol> = TypeConstructorEquality

    constructor(path: Path, typeParameters: List<TypeParameter>) : this(path.toString(OrbitMangler), typeParameters)
}

data class MetaType(val typeConstructor: TypeConstructor, val concreteTypeParameters: List<ValuePositionType>) : ValuePositionType, TypeExpression {
    companion object : KoinComponent {
        val invocation: Invocation by inject()
    }

    override val name: String
        get() = typeConstructor.name

    override val equalitySemantics: Equality<out TypeProtocol>
        get() = typeConstructor.equalitySemantics

    override fun evaluate(context: Context): TypeProtocol {
        // TODO - Verify concrete types satisfy typeConstructor's type parameters
        if (concreteTypeParameters.count() != typeConstructor.typeParameters.count())
            throw invocation.make("Type constructor expects ${typeConstructor.typeParameters.count()} type ${"parameter".pluralise(typeConstructor.typeParameters.count())}, found ${concreteTypeParameters.count()}")

        val paramsPath = Path(typeConstructor.name) + concreteTypeParameters.map { Path(it.name) }

        return Type(paramsPath, concreteTypeParameters)
    }
}

data class Parameter(override val name: String, val type: TypeProtocol) : TypeProtocol {
    override val equalitySemantics: Equality<out TypeProtocol> = type.equalitySemantics
}

interface SignatureProtocol<T: TypeProtocol> : ValuePositionType {
    val receiver: T
    val parameters: List<Parameter>
    val returnType: ValuePositionType

    fun toString(mangler: Mangler) : String
}

data class InstanceSignature(
    override val name: String,
    override val receiver: Parameter,
    override val parameters: List<Parameter>, override val returnType: ValuePositionType
) : SignatureProtocol<Parameter> {
    override val equalitySemantics: Equality<out TypeProtocol> = SignatureEquality

    override fun toString(mangler: Mangler): String {
        return mangler.mangle(this)
    }
}

data class TypeSignature(
    override val name: String,
    override val receiver: ValuePositionType,
    override val parameters: List<Parameter>,
    override val returnType: ValuePositionType
) : SignatureProtocol<ValuePositionType> {
    override val equalitySemantics: Equality<out TypeProtocol> = SignatureEquality

    override fun toString(mangler: Mangler): String {
        return mangler.mangle(this)
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

    enum class Infix(val leftType: TypeProtocol, val rightType: TypeProtocol, override val returnType: TypeProtocol, val symbol: String) :
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

enum class IntrinsicTypes(val type: ValuePositionType) {
    Unit(Type("Orb::Types::Intrinsics::Unit", isRequired = false)),
    Int(Type("Orb::Types::Intrinsics::Int", isRequired = false)),
    Symbol(Type("Orb::Types::Intrinsics::Symbol", isRequired = false)),
    Main(Type("Orb::Core::Main::Main", properties = listOf(Property("argc", Int.type)), isRequired = false)),
    BootstrapCoreStub(Type("Bootstrap::Core::Stub", isRequired = false)),
    Bool(Type("Orb::Types::Intrinsics::Bool", isRequired = false));

    companion object {
        val allTypes: Set<TypeProtocol>
            get() = values().map { it.type }.toSet()

        fun isIntrinsicType(path: Path) : Boolean {
            val mangled = OrbitMangler.mangle(path)

            return values()
                .map { it.type.name }
                .contains(mangled)
        }
    }

    val path: Path
        get() = name.toPath()
}