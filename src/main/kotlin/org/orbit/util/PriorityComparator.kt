package org.orbit.util

fun interface PriorityComparator<T> {
    fun compare(a: T, b: T) : T
}

fun <E> Iterable<E>.prioritise(comparator: PriorityComparator<E>?) : List<E> {
    return sortedWith { a, b ->
        when (comparator?.compare(a, b)) {
            a -> -1
            else -> 1
        }
    }
}