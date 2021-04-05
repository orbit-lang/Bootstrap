package org.orbit.util

fun <T> Collection<T>.containsAll(other: Collection<T>) : Boolean {
    return other.fold(true) { acc, next ->
        return@fold acc && contains(next)
    }
}

inline fun <reified U> Collection<*>.containsInstances() : Boolean {
    return filterIsInstance<U>().isNotEmpty()
}