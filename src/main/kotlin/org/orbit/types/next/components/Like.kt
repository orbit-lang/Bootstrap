package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.util.Printer

data class Like(val a: TypeComponent, val b: TypeComponent) : Constraint<Like>, KoinComponent {
    override val fullyQualifiedName: String = "(${a.fullyQualifiedName} : ${b.fullyQualifiedName})"
    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Context

    private val printer: Printer by inject()

    override fun sub(old: TypeComponent, new: TypeComponent) : Like {
        val nA = when (a.fullyQualifiedName) {
            old.fullyQualifiedName -> new
            else -> a
        }

        val nB = when (b.fullyQualifiedName) {
            old.fullyQualifiedName -> new
            else -> b
        }

        return Like(nA, nB)
    }

    override fun solve(ctx: Ctx): TypeComponent = when (b) {
        is ITrait -> when (StructuralEq.eq(ctx, b, a)) {
            true -> a
            else -> Never("Conformance Constraint does not hold `${a.toString(printer)} : ${b.toString(printer)}`")
        }

        else -> Never("${a.toString(printer)} cannot conform to non-Trait ${b.toString(printer)} (${b.kind.toString(printer)})")
    }

    override fun apply(inferenceUtil: InferenceUtil): InternalControlType = when (b) {
        is ITrait -> {
            inferenceUtil.addConformance(a, b)
            Anything
        }

        else -> Never("${a.toString(printer)} cannot conform to non-Trait ${b.toString(printer)} (${b.kind.toString(printer)})")
    }

    override fun conflicts(with: Constraint<*>): InternalControlType = when (with) {
        is Like -> when (fullyQualifiedName) {
            with.fullyQualifiedName -> Never("Duplicate Conformance Constraint: ${toString(printer)}")
            else -> Anything
        }

        else -> Anything
    }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("Not yet implemented")
    }
}