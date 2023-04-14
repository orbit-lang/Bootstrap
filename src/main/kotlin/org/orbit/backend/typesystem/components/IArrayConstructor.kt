package org.orbit.backend.typesystem.components

sealed interface IArrayConstructor : IConstructor<Array> {
    data class Empty(override val constructedType: Array) : IArrayConstructor {
        override val id: String = "[]"
        override val effects: List<Effect> = emptyList()

        override fun getDomain(): List<AnyType> = emptyList()
        override fun getCodomain(): AnyType = Array(constructedType.element, Array.Size.Fixed(0))
        override fun curry(): IArrow<*> = this

        override fun never(args: List<AnyType>): Never {
            TODO("Not yet implemented")
        }

        override fun substitute(substitution: Substitution): AnyType
            = Empty(constructedType.substitute(substitution) as Array)
    }

    data class Populated(override val constructedType: Array, private val dynamicSize: Int? = null) :
        IArrayConstructor {
        override val id: String = "[]"
        override val effects: List<Effect> = emptyList()

        override fun getDomain(): List<AnyType> {
            val size = dynamicSize ?: when (constructedType.size) {
                is Array.Size.Fixed -> constructedType.size.size
                else -> TODO("WEIRD ARRAY ERROR")
            }

            return (0 until size).map { constructedType.element }
        }

        override fun getCodomain(): AnyType = constructedType
        override fun curry(): IArrow<*> = this

        override fun never(args: List<AnyType>): Never {
            TODO("Not yet implemented")
        }

        override fun substitute(substitution: Substitution): AnyType
            = Populated(constructedType.substitute(substitution) as Array)
    }
}