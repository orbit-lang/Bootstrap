package org.orbit.frontend.extensions

operator fun IntRange.minus(offset: Int) : IntRange {
    return IntRange(start - offset, endInclusive - offset)
}

fun IntRange.distance() : Int {
    return (endInclusive - start) + 1
}