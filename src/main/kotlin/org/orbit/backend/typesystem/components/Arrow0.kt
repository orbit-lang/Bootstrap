package org.orbit.backend.typesystem.components

data class Arrow0(val gives: AnyType, override val effects: List<Effect>) : IArrow<Arrow0> {
    override val id: String = "() -> ${gives.id}"

    override fun getDomain(): List<AnyType> = emptyList()
    override fun getCodomain(): AnyType = gives

    override fun substitute(substitution: Substitution): Arrow0
        = Arrow0(gives.substitute(substitution), effects.substitute(substitution) as List<Effect>)
    override fun curry(): Arrow0 = this
    override fun never(args: List<AnyType>): Never = Never("Unreachable")

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
        = Arrow0(gives.flatten(from, env), effects)

    override fun equals(other: Any?): Boolean = when (other) {
        is Arrow0 -> gives == other.gives
        else -> false
    }

    override fun toString(): String = prettyPrint()
}