package org.orbit.types.next.constraints

import org.orbit.types.next.components.Ctx
import org.orbit.types.next.components.TypeComponent

interface ConstraintApplication<T: TypeComponent> {
    val initialValue: T
}

interface Constraint<T: TypeComponent, C: ConstraintApplication<T>> {
    fun refine(ctx: Ctx, input: T) : C?
}