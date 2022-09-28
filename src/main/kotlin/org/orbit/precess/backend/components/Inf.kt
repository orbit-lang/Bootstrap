package org.orbit.precess.backend.components

import org.orbit.backend.typesystem.components.AnyType
import org.orbit.backend.typesystem.components.Env

interface Inf<E : Expr<E>> {
    fun infer(env: Env): AnyType
}