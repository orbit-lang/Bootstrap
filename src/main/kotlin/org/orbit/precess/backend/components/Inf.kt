package org.orbit.precess.backend.components

interface Inf<E : Expr<E>> {
    fun infer(env: Env): IType<*>
}