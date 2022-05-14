package org.orbit.types.next.components

import org.orbit.types.next.inference.InferenceUtil

interface Constraint<Self: Constraint<Self>> : TypeComponent {
    fun sub(old: TypeComponent, new: TypeComponent) : Self
    fun solve(ctx: Ctx) : TypeComponent
    fun apply(inferenceUtil: InferenceUtil) : InternalControlType
    fun conflicts(with: Constraint<*>) : InternalControlType
}