package org.orbit.types.next.utils

import org.orbit.types.next.components.Constraint
import org.orbit.types.next.components.TypeComponent

fun <C: Constraint<C>> C.sub(vararg subs: Pair<TypeComponent, TypeComponent>) : C
    = subs.fold(this) { acc, next -> acc.sub(next.first, next.second) }