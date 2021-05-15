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

// Same as partial, but allows different ordering of args
fun <A, B, C> partialAlt(fn: (A, B) -> C, a: A) : (B) -> C {
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

fun <A, B, C, D> partialAlt(fn: (A, B, C) -> D, a: A, b: B) : (C) -> D {
    return {
        fn(a, b, it)
    }
}

typealias Fn<A, B, C> = (A, B) -> C

operator fun <A, B, C> Fn<A, B, C>.plus(param: B) : (A) -> C {
    return partial(this, param)
}

fun String.pluralise(count: Int) : String = when (count) {
    1 -> this
    else -> this + "s"
}

fun String.toPath(mangler: Mangler = OrbitMangler) : Path = mangler.unmangle(this)
