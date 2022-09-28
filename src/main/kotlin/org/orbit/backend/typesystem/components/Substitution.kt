package org.orbit.backend.typesystem.components

data class Substitution(val old: AnyType, val new: AnyType)

fun List<AnyType>.substituteAll(substitution: Substitution) : List<AnyType>
    = map { it.substitute(substitution) }

interface Substitutable {
    fun substitute(substitution: Substitution): AnyType
}