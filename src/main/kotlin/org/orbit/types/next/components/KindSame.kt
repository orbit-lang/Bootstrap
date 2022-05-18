package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.util.Printer

data class KindSame(val a: TypeComponent, val b: TypeComponent) : Constraint<KindSame>, KoinComponent {
    override val fullyQualifiedName: String = "${a.fullyQualifiedName} ^ ${b.fullyQualifiedName}"
    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Context

    private val printer: Printer by inject()

    override fun solve(ctx: Ctx): TypeComponent = when (KindEq.eq(ctx, a.kind, b.kind)) {
        true -> b
        else -> Never("Kind Equality Constraint does not hold for `${a.toString(printer)} ^ ${b.toString(printer)}`")
    }

    // TODO - Is it useful to know that a ^ b in subsequent Constraints?
    override fun apply(inferenceUtil: InferenceUtil): InternalControlType = Anything

    override fun sub(old: TypeComponent, new: TypeComponent): KindSame {
        val nA = when (a.fullyQualifiedName) {
            old.fullyQualifiedName -> new
            else -> a
        }

        val nB = when (b.fullyQualifiedName) {
            old.fullyQualifiedName -> new
            else -> b
        }

        return KindSame(nA, nB)
    }

    // TODO - e.g. KindEq [A, Kinds::Value] conflicts with KindEq [A, Kinds::Type] - they can't both be true
    override fun conflicts(with: Constraint<*>): InternalControlType = Anything

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("Not yet implemented")
    }
}
