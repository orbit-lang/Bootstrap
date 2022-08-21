package org.orbit.precess.backend.components

data class Substitution(val old: IType<*>, val new: IType<*>)

fun <S: Substitutable<S>> List<S>.substituteAll(substitution: Substitution) : List<S>
    = map { it.substitute(substitution) }

interface Substitutable<Self : Substitutable<Self>> {
    fun substitute(substitution: Substitution): Self
}