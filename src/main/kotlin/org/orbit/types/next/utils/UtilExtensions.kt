package org.orbit.types.next.utils

import org.orbit.types.next.components.Constraint
import org.orbit.types.next.components.TypeComponent

fun <C: Constraint<C>> C.sub(vararg subs: Pair<TypeComponent, TypeComponent>) : C
    = subs.fold(this) { acc, next -> acc.sub(next.first, next.second) }

fun <T> Collection<T>.onlyOrNull(predicate: (T) -> Boolean) : T? {
    val results = filter(predicate)

    return when (results.count()) {
        1 -> results.first()
        else -> null
    }
}

fun <T> Collection<T>.only(predicate: (T) -> Boolean) : T
    = onlyOrNull(predicate)!!

fun <T, U> Collection<T>.mapOnly(predicate: (T) -> Boolean, transform: (T) -> U) : U
    = transform(only(predicate))