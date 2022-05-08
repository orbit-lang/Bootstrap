package org.orbit.types.next.constraints

import org.orbit.types.next.components.*
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.types.next.intrinsics.Native
import org.orbit.util.Printer

interface ConstraintApplication<T: TypeComponent> {
    val initialValue: T

    fun resultValue() : TypeComponent
}

interface Constraint<T: TypeComponent, C: ConstraintApplication<T>> : TypeComponent {
    fun refine(inferenceUtil: InferenceUtil, input: T) : ConstraintApplication<T>?

    override val isSynthetic: Boolean get() = false
    override val kind: Kind get() = IntrinsicKinds.Type

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation
        = TypeRelation.Unrelated(this, other)
}
