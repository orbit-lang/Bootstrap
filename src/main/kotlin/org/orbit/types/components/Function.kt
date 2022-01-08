package org.orbit.types.components

class Function(private val canonicalName: String, val inputTypes: List<TypeProtocol>, val outputType: TypeProtocol) : Entity(canonicalName, equalitySemantics = NominalEquality) {
    override val kind: TypeKind = FunctionKind
    override val name: String = "(${inputTypes.joinToString(",") { it.name }}) -> ${outputType.name}"

    private fun curry(type: TypeProtocol) : TypeProtocol = when (type) {
        is Function -> type.curry()
        else -> type
    }

    fun curry() : Lambda {
        if (inputTypes.size == 1) return Lambda(curry(inputTypes[0]), curry(outputType))

        val rhs = Lambda(inputTypes.last(), curry(outputType))

        return Function(canonicalName, inputTypes.dropLast(1), rhs)
            .curry()
    }

    fun uncurry() : Function = Function("->", inputTypes.map { when (it) {
        is Lambda -> it.uncurry()
        is Function -> it.uncurry()
        else -> it
    }}, outputType)
}

interface Operator : TypeProtocol {
    val symbol: String
    val resultType: TypeProtocol
}

data class InfixOperator(override val symbol: String, val leftType: TypeProtocol, val rightType: TypeProtocol, override val resultType: TypeProtocol) :
    Operator {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = leftType.equalitySemantics
    override val name: String = "${leftType.name}${symbol}${rightType.name}"
    override val kind: TypeKind = FunctionKind
}

data class PrefixOperator(override val symbol: String, val operandType: TypeProtocol, override val resultType: TypeProtocol) :
    Operator {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = operandType.equalitySemantics
    override val name: String = "$symbol${operandType.name}"
    override val kind: TypeKind = FunctionKind
}

data class PostfixOperator(override val symbol: String, val operandType: TypeProtocol, override val resultType: TypeProtocol) :
    Operator {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = operandType.equalitySemantics
    override val name: String = "${operandType.name}$symbol"
    override val kind: TypeKind = FunctionKind
}