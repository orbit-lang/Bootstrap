package org.orbit.backend.typesystem.components

data class StructConstructor(override val constructedType: Struct, val args: List<AnyType>) : IConstructor<Struct> {
    override val id: String = "(${args.joinToString(", ") { it.id }}) -> ${constructedType.id}"
    override val effects: List<Effect> = emptyList()

    override fun getDomain(): List<AnyType> = args
    override fun getCodomain(): AnyType = constructedType
    override fun getCardinality(): ITypeCardinality
        = constructedType.getCardinality()

    override fun curry(): IArrow<*> = this

    override fun substitute(substitution: Substitution): IConstructor<Struct> =
        StructConstructor(constructedType.substitute(substitution), args.map { it.substitute(substitution) })

    override fun never(args: List<AnyType>): Never =
        Never("Cannot construct Type $constructedType with arguments (${args.joinToString("; ")})")

    override fun toString(): String = prettyPrint()
}