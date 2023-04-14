package org.orbit.backend.typesystem.components

data class Case(val condition: AnyType, val result: AnyType) : IArrow<Case> {
    override val id: String = "case (${condition.id}) -> ${result.id}"
    override val effects: List<Effect> = emptyList()

    override fun getCardinality(): ITypeCardinality
        = condition.getCardinality()

    override fun getDomain(): List<AnyType> = listOf(condition)
    override fun getCodomain(): AnyType = result

    override fun never(args: List<AnyType>): Never {
        TODO("Not yet implemented")
    }

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
        = Case(condition.flatten(from, env), result.flatten(from, env))

    override fun curry(): IArrow<*> = this
    override fun substitute(substitution: Substitution): Case
        = Case(condition.substitute(substitution), result.substitute(substitution))

    override fun equals(other: Any?): Boolean = when (other) {
        is Case -> other.condition == condition && other.result == result
        else -> false
    }

    override fun prettyPrint(depth: Int): String
        = "case ($condition) -> $result"

    override fun toString(): String = prettyPrint()
}