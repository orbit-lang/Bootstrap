package org.orbit.types

class Function(val inputTypes: List<TypeProtocol>, val outputType: TypeProtocol) : TypeProtocol {
    override val equalitySemantics: Equality<out TypeProtocol> = SignatureEquality

    override val name: String = "(${inputTypes.joinToString(",") { it.name }}) -> ${outputType.name}"
    val behaviours: List<Behaviour> = emptyList()

    private fun curry(type: TypeProtocol) : TypeProtocol = when (type) {
        is Function -> type.curry()
        else -> type
    }

    fun curry() : Lambda {
        if (inputTypes.size == 1) return Lambda(curry(inputTypes[0]), curry(outputType))

        val rhs = Lambda(inputTypes.last(), curry(outputType))

        return Function(inputTypes.dropLast(1), rhs)
            .curry()
    }
}