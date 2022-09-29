package org.orbit.backend.typesystem.components

data class Substitution(val old: AnyType, val new: AnyType) {
    constructor(pair: Pair<AnyType, AnyType>) : this(pair.first, pair.second)
}

fun <S> List<Substitutable<S>>.substitute(substitution: Substitution) : List<S>
    = map { it.substitute(substitution) }

interface Substitutable<T> {
    fun substitute(substitution: Substitution): T
}