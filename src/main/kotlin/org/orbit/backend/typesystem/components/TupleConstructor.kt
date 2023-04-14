package org.orbit.backend.typesystem.components

data class TupleConstructor(val left: AnyType, val right: AnyType, override val constructedType: Tuple) :
    IConstructor<Tuple> {
    override val effects: List<Effect> = emptyList()

    override fun getDomain(): List<AnyType>
        = listOf(left, right)

    override fun getCodomain(): AnyType = constructedType

    override fun curry(): IArrow<*> = this

    override fun never(args: List<AnyType>): Never {
        TODO("Not yet implemented")
    }

    override val id: String = "(${left.id} * ${left.id}) -> ${constructedType.id}"

    override fun substitute(substitution: Substitution): IConstructor<Tuple>
        = TupleConstructor(
        left.substitute(substitution),
        right.substitute(substitution),
        constructedType.substitute(substitution)
    )

    override fun prettyPrint(depth: Int): String
        = Arrow2(left, right, constructedType, effects).prettyPrint(depth)

    override fun toString(): String
        = prettyPrint()
}