package org.orbit.backend.typesystem.components

data class Substitution(val old: AnyType, val new: AnyType) {
    constructor(pair: Pair<AnyType, AnyType>) : this(pair.first, pair.second)
}

fun List<AnyType>.substituteAll(substitution: Substitution) : List<AnyType>
    = map { it.substitute(substitution) }

interface Substitutable {
    fun substitute(substitution: Substitution): AnyType
}