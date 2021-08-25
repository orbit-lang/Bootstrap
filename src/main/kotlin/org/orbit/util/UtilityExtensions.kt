package org.orbit.util

import org.orbit.core.Mangler
import org.orbit.core.OrbitMangler
import org.orbit.core.Path

fun <T> Collection<T>.containsAll(other: Collection<T>) : Boolean {
    return other.fold(true) { acc, next ->
        return@fold acc && contains(next)
    }
}

inline fun <reified U> Collection<*>.containsInstances() : Boolean {
    return filterIsInstance<U>().isNotEmpty()
}

fun <A, B, C> partial(fn: (A, B) -> C, b: B) : (A) -> C {
    return {
        fn(it, b)
    }
}

infix fun <A, B, C> ((A, B) -> C).apply(b: B) : (A) -> C
    = partial(this, b)

// Same as partial, but allows different ordering of args
fun <A, B, C> partialReverse(fn: (A, B) -> C, a: A) : (B) -> C {
    return {
        fn(a, it)
    }
}

fun <A, B, C, D> partial(fn: (A, B, C) -> D, b: B, c: C) : (A) -> D {
    return {
        fn(it, b, c)
    }
}

fun <A, B, C, D, E> partial(fn: (A, B, C, D) -> E, b: B, c: C, d: D) : (A) -> E {
    return {
        fn(it, b, c, d)
    }
}

fun <A, B, C, D> partialReverse(fn: (A, B, C) -> D, a: A, b: B) : (C) -> D {
    return {
        fn(a, b, it)
    }
}

fun <A, B> dispose(fn: (A) -> B) : (A) -> Unit {
    return {
        fn(it)
    }
}

typealias AnyFn<A, B> = (A) -> B

operator fun <A, B> AnyFn<A, B>.unaryMinus() : (A) -> Unit = dispose(this)

typealias Fn<A, B, C> = (A, B) -> C
typealias Fn3<A, B, C, D> = (A, B, C) -> D

operator fun <A, B, C> Fn<A, B, C>.plus(param: B) : (A) -> C {
    return partial(this, param)
}

operator fun <A, B, C> ((A) -> B).plus(c: C) : (A) -> C {
    return this + c
}

fun String.pluralise(count: Int) : String = when (count) {
    1 -> this
    else -> this + "s"
}

fun String.toPath(mangler: Mangler = OrbitMangler) : Path = mangler.unmangle(this)

fun <T> Collection<T>.startsWith(element: T) : Boolean {
    return firstOrNull() == element
}

fun <T> Collection<T>.endsWith(element: T) : Boolean {
    return lastOrNull() == element
}

fun <T, U> T.pairMap(transform: (T) -> U) : Pair<T, U> {
    val transformedValue = transform(this)
    return Pair(this, transformedValue)
}

fun <T, U> Collection<T>.pairMapAll(transform: (T) -> U) : List<Pair<T, U>> {
    return map { it.pairMap(transform) }
}

fun <T, U> Collection<T>.flatPairMap(transform: (T) -> List<U>) : List<Pair<T, U>> = flatMap { elem ->
    transform(elem).map { Pair(elem, it) }
}

fun <T, U, V> Pair<T, U>.flatten(into: (T, U) -> V) : V {
    return into(first, second)
}

fun <T, U, V> Pair<T, U>.unflatten(into: (U, T) -> V) : V {
    return into(second, first)
}

fun <T, U> Pair<T, U>.reverseFlatten() : Pair<U, T> {
    return Pair(second, first)
}

fun <T, U> Collection<T>.cartesian(other: Collection<U>) : Collection<Pair<T, U>> {
    return flatMap { lhs ->
        other.map { rhs -> lhs to rhs }
    }
}

fun <T> Collection<T>.cartesian() : Collection<Pair<T, T>> {
    return flatMap { a -> map { b -> Pair(a, b) } }
}
