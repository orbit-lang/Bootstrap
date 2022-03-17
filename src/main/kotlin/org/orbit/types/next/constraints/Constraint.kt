package org.orbit.types.next.constraints

import org.orbit.types.next.components.Ctx
import org.orbit.types.next.components.IType

interface ConstraintApplication<T: IType> {
    val initialValue: T
}

interface Constraint<T: IType, C: ConstraintApplication<T>> {
    fun refine(ctx: Ctx, input: T) : C?
}