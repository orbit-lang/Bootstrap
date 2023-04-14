package org.orbit.backend.typesystem.components

data class Arrow3(val a: AnyType, val b: AnyType, val c: AnyType, val gives: AnyType, override val effects: List<Effect>) :
    IArrow<Arrow3> {
    override val id: String = "(${a.id}, ${b.id}, ${c.id}) -> ${gives.id}"

    override fun getDomain(): List<AnyType> = listOf(a, b, c)
    override fun getCodomain(): AnyType = gives

    override fun substitute(substitution: Substitution): Arrow3 = Arrow3(
        a.substitute(substitution),
        b.substitute(substitution),
        c.substitute(substitution),
        gives.substitute(substitution),
        effects.substitute(substitution) as List<Effect>
    )

    override fun curry(): Arrow2 = Arrow2(a, b, Arrow1(c, gives, emptyList()), effects)

    override fun never(args: List<AnyType>): Never =
        Never("$id expects arguments of ($a, $b, $c), found (${args.joinToString(", ")})")

    override fun toString(): String = prettyPrint()
}