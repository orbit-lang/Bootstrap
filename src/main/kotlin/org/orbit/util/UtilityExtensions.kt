package org.orbit.util

import org.orbit.core.INameMangler
import org.orbit.core.OrbitMangler
import org.orbit.core.Path

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

fun <A, B, C, D, E> partial(fn: (A, B, C, D) -> E, b: B, c: C, d: D) : (A) -> E {
    return {
        fn(it, b, c, d)
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

operator fun <A, B, C> Fn<A, B, C>.plus(param: B) : (A) -> C {
    return partial(this, param)
}

operator fun <A, B, C> ((A) -> B).plus(c: C) : (A) -> C {
    return this + c
}

fun String.toPath(mangler: INameMangler = OrbitMangler) : Path = mangler.unmangle(this)

fun <T> Collection<T>.endsWith(element: T) : Boolean {
    return lastOrNull() == element
}

fun <T, U> T.pairMap(transform: (T) -> U) : Pair<T, U> {
    val transformedValue = transform(this)
    return Pair(this, transformedValue)
}

fun <T, U> T.pairMapOrNull(transform: (T) -> U?) : Pair<T, U>? {
    val transformedValue = transform(this) ?: return null
    return Pair(this, transformedValue)
}

fun <T, U> Collection<T>.pairMapAll(transform: (T) -> U) : List<Pair<T, U>> {
    return map { it.pairMap(transform) }
}

fun <T, U, V> Pair<T, U>.flatten(into: (T, U) -> V) : V {
    return into(first, second)
}

fun <A, B> Collection<A>.cartesianProduct(other: Collection<B>) : Sequence<Pair<A, B>> = sequence {
    forEach { a -> other.forEach { b -> yield(Pair(a, b)) } }
}
