package org.orbit.backend.typesystem.components

data class Arrow2(val a: AnyType, val b: AnyType, val gives: AnyType, override val effects: List<Effect>) :
    IArrow<Arrow2> {
    override val id: String = "(${a.id}, ${b.id}) -> ${gives.id}"

    override fun getDomain(): List<AnyType> = listOf(a, b)
    override fun getCodomain(): AnyType = gives

    override fun substitute(substitution: Substitution): Arrow2 =
        Arrow2(
            a.substitute(substitution),
            b.substitute(substitution),
            gives.substitute(substitution),
            effects.substitute(substitution) as List<Effect>
        )

    override fun curry(): Arrow1 = Arrow1(a, Arrow1(b, gives, emptyList()), effects)

    override fun never(args: List<AnyType>): Never =
        Never("$this expects arguments of ($a, $b), found (${args.joinToString(", ")})")

    override fun toString(): String = prettyPrint()
}