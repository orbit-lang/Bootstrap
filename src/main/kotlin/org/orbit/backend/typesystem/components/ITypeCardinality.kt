package org.orbit.backend.typesystem.components

sealed interface ITypeCardinality {
    object Zero : ITypeCardinality {
        override fun plus(other: ITypeCardinality): ITypeCardinality = when (other) {
            is Finite -> Finite(other.count)
            is Infinite -> Infinite
            is Mono -> Finite(1)
            is Zero -> this
        }
    }

    object Mono : ITypeCardinality {
        override fun plus(other: ITypeCardinality): ITypeCardinality = when (other) {
            is Mono -> Finite(2)
            is Finite -> Finite(other.count + 1)
            is Infinite -> Infinite
            else -> this
        }
    }

    object Infinite : ITypeCardinality {
        override fun plus(other: ITypeCardinality): ITypeCardinality
            = this
    }

    data class Finite(val count: Int) : ITypeCardinality {
        override fun plus(other: ITypeCardinality): ITypeCardinality = when (other) {
            is Finite -> Finite(count + other.count)
            is Infinite -> Infinite
            is Mono -> Finite(count + 1)
            is Zero -> this
        }
    }

    operator fun plus(other: ITypeCardinality): ITypeCardinality
}