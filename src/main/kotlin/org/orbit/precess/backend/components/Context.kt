package org.orbit.precess.backend.components

class Context(private val initialEnv: Env, val typeVariables: List<IType.TypeVar>, val clauses: List<Clause>) {
    fun initialise(): Env = clauses.fold(initialEnv) { acc, next -> next.weaken(acc) }
}