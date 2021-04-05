package org.orbit.types

class Function(val inputTypes: List<Type>, val outputType: Type) : Type {
    override val name: String = "(${inputTypes.joinToString(",") { it.name }}) -> ${outputType.name}"
    override val behaviours: List<Behaviour> = emptyList()
    override val members: List<Member> = emptyList()

    private fun curry(type: Type) : Type = when (type) {
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