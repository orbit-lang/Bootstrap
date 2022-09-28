package org.orbit.precess.backend.components

import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType

class Context(private val initialEnv: Env, val typeVariables: List<IType.TypeVar>, val clauses: List<Clause>) {
    fun initialise(): Env = clauses.fold(initialEnv) { acc, next -> next.weaken(acc) }
}