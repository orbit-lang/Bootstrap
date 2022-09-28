package org.orbit.precess.backend.components

import org.orbit.precess.backend.utils.AnyType

data class Substitution(val old: AnyType, val new: AnyType)

fun List<IType.SubstitutableType>.substituteAll(substitution: Substitution) : List<IType.SubstitutableType>
    = map { it.substitute(substitution) }

interface Substitutable {
    fun substitute(substitution: Substitution): AnyType
}