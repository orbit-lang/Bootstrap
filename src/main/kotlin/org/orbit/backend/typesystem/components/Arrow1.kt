package org.orbit.backend.typesystem.components

data class Arrow1(val takes: AnyType, val gives: AnyType, override val effects: List<Effect>) : IArrow<Arrow1> {
    override val id: String = "(${takes.id}) -> ${gives.id}"

    override fun getDomain(): List<AnyType> = listOf(takes)
    override fun getCodomain(): AnyType = gives

    override fun substitute(substitution: Substitution): Arrow1 =
        Arrow1(
            takes.substitute(substitution),
            gives.substitute(substitution),
            effects.substitute(substitution) as List<Effect>
        )

    override fun curry(): Arrow0
        = Arrow0(Arrow1(takes, gives, emptyList()), effects)

    override fun never(args: List<AnyType>): Never =
        Never("$id expects argument of Type $takes, found $args[0]")

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType {
        val domain = takes.flatten(from, env)

        if (domain is Never) return domain

        val codomain = gives.flatten(from, env)

        if (domain is Never) return codomain

        return Arrow1(domain, codomain, effects)
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is Arrow1 -> takes == other.takes && gives == other.gives
        is Safe -> when (val t = other.type) {
            is Arrow1 -> this == t
            else -> false
        }
        else -> false
    }

    override fun toString(): String = prettyPrint()
}