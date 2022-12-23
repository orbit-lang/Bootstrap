package org.orbit.backend.typesystem.components

data class Substitution(val old: AnyType, val new: AnyType) {
    constructor(pair: Pair<AnyType, AnyType>) : this(pair.first, pair.second)

    override fun equals(other: Any?): Boolean = when (other) {
        is Substitution -> other.old.getCanonicalName() == old.getCanonicalName() && other.new.getCanonicalName() == new.getCanonicalName()
        else -> false
    }
}

fun <S> Collection<Substitutable<S>>.substitute(substitution: Substitution) : List<S>
    = map { it.substitute(substitution) }

interface Substitutable<T> {
    fun substitute(substitution: Substitution): T
}