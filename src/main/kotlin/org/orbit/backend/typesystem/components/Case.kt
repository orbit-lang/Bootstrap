package org.orbit.backend.typesystem.components

data class ElseCase(val result: AnyType) : IType {
    override val id: String = "case else -> $result"

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Mono

    override fun substitute(substitution: Substitution): AnyType
        = ElseCase(result.substitute(substitution))

    override fun equals(other: Any?): Boolean = when (other) {
        is ElseCase -> other.result == result
        else -> false
    }

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)

        return "${indent}case else -> $result"
    }

    override fun toString(): String
        = prettyPrint()
}

data class Case(val condition: AnyType, val result: AnyType) : IType {
    override val id: String = "case $condition -> $result"

    fun eraseResult() : Case
        = Case(condition, result.erase())

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Mono

    override fun substitute(substitution: Substitution): AnyType
        = Case(condition.substitute(substitution), result.substitute(substitution))

    override fun equals(other: Any?): Boolean = when (other) {
        is Case -> other.condition == condition && other.result == result
        else -> false
    }

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)

        return "${indent}case $condition -> $result"
    }

    override fun toString(): String
        = prettyPrint()
}