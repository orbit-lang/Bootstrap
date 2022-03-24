package org.orbit.types.next.constraints

import org.orbit.types.next.components.*
import org.orbit.util.Printer

interface ConstraintApplication<T: TypeComponent> {
    val initialValue: T
}

interface Constraint<T: TypeComponent, C: ConstraintApplication<T>> {
    fun refine(ctx: Ctx, input: T) : C?
}

data class ParameterConstraint(val target: TypeComponent, val eq: ITypeEq<*, *>)
