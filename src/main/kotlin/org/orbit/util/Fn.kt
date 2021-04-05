package org.orbit.util

interface Monoid<T> {
    fun T.combine(other: T) : T
    fun T.zero() : T
}