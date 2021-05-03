package org.orbit.util

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

typealias Fn<A, B, C> = (A, B) -> C

operator fun <A, B, C> Fn<A, B, C>.plus(param: B) : (A) -> C {
    return partial(this, param)
}

fun String.pluralise(count: Int) : String = when (count) {
    1 -> this
    else -> this + "s"
}