package org.orbit.types

import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.plus
import org.orbit.util.toPath

abstract class Entity(
    override val name: String,
    open val properties: List<Property> = emptyList(),
    override val equalitySemantics: Equality<Entity>
) : ValuePositionType {
    constructor(path: Path, properties: List<Property> = emptyList(), equalitySemantics: Equality<Entity>)
        : this(path.toString(OrbitMangler), properties, equalitySemantics)

    override fun equals(other: Any?): Boolean = when (other) {
        is Entity -> name == other.name
        else -> false
    }
}

data class Type(override val name: String, override val properties: List<Property> = emptyList(), override val equalitySemantics: Equality<Entity> = NominalEquality) : Entity(name, properties, equalitySemantics) {
    constructor(path: Path, properties: List<Property> = emptyList(), equalitySemantics: Equality<Entity> = NominalEquality)
            : this(path.toString(OrbitMangler), properties, equalitySemantics)
}

data class Trait(override val name: String, override val properties: List<Property> = emptyList(), override val equalitySemantics: Equality<Entity> = StructuralEquality) : Entity(name, properties,equalitySemantics) {
    constructor(path: Path, properties: List<Property> = emptyList(), equalitySemantics: Equality<Entity> = StructuralEquality)
            : this(path.toString(OrbitMangler), properties, equalitySemantics)
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

    enum class Infix(val leftType: TypeProtocol, val rightType: TypeProtocol, override val returnType: TypeProtocol, val symbol: String) : IntrinsicOperators {
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
    Unit(Type("Orb::Types::Intrinsics::Unit")),
    Int(Type("Orb::Types::Intrinsics::Int")),
    Symbol(Type("Orb::Types::Intrinsics::Symbol")),
    Main(Type("Orb::Core::Main::Main", listOf(Property("argc", IntrinsicTypes.Int.type))));

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