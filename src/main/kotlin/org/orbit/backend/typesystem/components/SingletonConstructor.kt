package org.orbit.backend.typesystem.components

data class SingletonConstructor(val type: AnyType) : IConstructor<AnyType> {
    override val id: String = "() -> ${type.id}"
    override val constructedType: AnyType = type
    override val effects: List<Effect> = emptyList()

    override fun getDomain(): List<AnyType> = emptyList()
    override fun getCodomain(): AnyType = type
    override fun curry(): IArrow<*> = this

    override fun never(args: List<AnyType>): Never {
        TODO("Not yet implemented")
    }

    override fun substitute(substitution: Substitution): AnyType
        = SingletonConstructor(type.substitute(substitution))
}