package org.orbit.backend.typesystem.components

private const val MAX_VARIADIC_COUNT = 100

sealed interface VariadicBound {
    fun isSatisfied(by: Int) : Boolean
    fun maxSize() : Int

    object Any : VariadicBound {
        override fun isSatisfied(by: Int): Boolean = true
        override fun maxSize(): Int = MAX_VARIADIC_COUNT

        override fun toString(): String = "variadic(*)"
    }

    data class Exactly(val bound: Int) : VariadicBound {
        override fun isSatisfied(by: Int): Boolean = by == bound
        override fun maxSize(): Int = bound

        override fun toString(): String = "variadic($bound)"
    }

    data class AtLeast(val bound: Int) : VariadicBound {
        override fun isSatisfied(by: Int): Boolean = by > bound
        override fun maxSize(): Int = MAX_VARIADIC_COUNT

        override fun toString(): String = "variadic(at least $bound)"
    }

    data class AtMost(val bound: Int) : VariadicBound {
        override fun isSatisfied(by: Int): Boolean = by < bound
        override fun maxSize(): Int = bound

        override fun toString(): String = "variadic(at most $bound)"
    }

    data class Between(val lowerBound: Int, val upperBound: Int, val inclusive: Boolean) : VariadicBound {
        override fun isSatisfied(by: Int): Boolean = when (inclusive) {
            true -> by in lowerBound..upperBound
            else -> by in (lowerBound + 1) until upperBound
        }

        override fun maxSize(): Int = when (inclusive) {
            true -> upperBound
            else -> upperBound - 1
        }

        override fun toString(): String = "variadic(between $lowerBound and $upperBound)"
    }
}