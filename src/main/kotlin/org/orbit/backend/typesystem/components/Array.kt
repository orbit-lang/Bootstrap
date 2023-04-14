package org.orbit.backend.typesystem.components

data class Array(val element: AnyType, val size: Size) : IType, ISpecialisedType, IConstructableType<Array> {
    sealed interface Size {
        data class Fixed(val size: Int) : Size {
            override fun equals(other: kotlin.Any?): Boolean = when (other) {
                is Fixed -> other.size == size
                is Any -> true
                else -> false
            }

            override fun hashCode(): Int = size

            override fun toString(): String = "$size"
        }

        object Any : Size {
            override fun equals(other: kotlin.Any?): Boolean = when (other) {
                is Size -> true
                else -> false
            }

            override fun toString(): String = "∞"
        }
    }

    override val id: String = "[${element.id}/$size]"

    override fun getConstructors(): List<IConstructor<*>> = when (size) {
        is Size.Fixed -> when (size.size) {
            0 -> listOf(IArrayConstructor.Empty(this))
            else -> listOf(IArrayConstructor.Empty(this), IArrayConstructor.Populated(this))
        }

        is Size.Any -> listOf(IArrayConstructor.Empty(this))
    }

    override fun getConstructor(given: List<AnyType>): IConstructor<Array>? = when (given.count()) {
        0 -> getConstructors()[0] as IConstructor<Array>
        else -> when (size) {
            is Size.Fixed -> getConstructors()[1] as IConstructor<Array>
            is Size.Any -> IArrayConstructor.Populated(this, given.count())
        }
    }

    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Infinite
    override fun substitute(substitution: Substitution): AnyType
        = Array(element.substitute(substitution), size)

    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = element.getUnsolvedTypeVariables()

    override fun isSpecialised(): Boolean = when (element) {
        is ISpecialisedType -> element.isSpecialised()
        else -> false
    }

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)

        return "$indent[$element/$size]"
    }

    override fun toString(): String
        = prettyPrint()
}