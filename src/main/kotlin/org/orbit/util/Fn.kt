package org.orbit.util

interface Semigroup<T> {
    operator fun plus(other: T) : T
}

fun <T: Semigroup<T>> Collection<T>.fold(initial: T) : T
    = fold(initial) { acc, next -> acc + next }

interface Monoid<T> {
    fun T.combine(other: T) : T
    fun T.zero() : T
}

sealed class Either<L, R> {
    data class Left<T>(val value: T) : Either<T, Nothing>()
    data class Right<T>(val value: T) : Either<Nothing, T>()
}
