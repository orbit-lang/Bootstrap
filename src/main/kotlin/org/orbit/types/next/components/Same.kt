package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.types.next.inference.InferenceUtil
import org.orbit.util.Printer

data class Same(val a: TypeComponent, val b: TypeComponent) : Constraint<Same>, KoinComponent {
    override val fullyQualifiedName: String = "(${a.fullyQualifiedName} = ${b.fullyQualifiedName})"
    override val isSynthetic: Boolean = false
    override val kind: Kind = IntrinsicKinds.Context

    private val printer: Printer by inject()

    override fun sub(old: TypeComponent, new: TypeComponent) : Same {
        val nA = when (a.fullyQualifiedName) {
            old.fullyQualifiedName -> new
            else -> a
        }

        val nB = when (b.fullyQualifiedName) {
            old.fullyQualifiedName -> new
            else -> b
        }

        return Same(nA, nB)
    }

    override fun solve(ctx: Ctx): TypeComponent = when (AnyEq.eq(ctx, a, b)) {
        true -> b
        else -> Never("Equality Constraint does not hold `${a.toString(printer)} = ${b.toString(printer)}`")
    }

    override fun apply(inferenceUtil: InferenceUtil) : InternalControlType = when (b) {
        a -> Anything
        else -> {
            inferenceUtil.declare(Alias(a.fullyQualifiedName, b))
            Anything
        }
    }

//        when (b) {
//        is ITypeParameter -> Never("Type ${a.toString(printer)} is not a concrete Type in this context")
//        else -> {
//            inferenceUtil.declare(Alias(a.fullyQualifiedName, b))
//
//            Anything
//        }
//    }

    override fun conflicts(with: Constraint<*>): InternalControlType = when (with) {
        is Same -> when {
            fullyQualifiedName == with.fullyQualifiedName -> Never("Duplicate Equality Constraint: ${toString(printer)}")
            a.fullyQualifiedName == with.a.fullyQualifiedName -> when (b.fullyQualifiedName) {
                with.b.fullyQualifiedName -> Anything
                else -> Never("Conflicting Equality Constraints `${toString(printer)}` & `${with.toString(printer)}`")
            }

            a.fullyQualifiedName == with.b.fullyQualifiedName -> when (b.fullyQualifiedName) {
                with.a.fullyQualifiedName -> Never("Conflicting Equality Constraints `${toString(printer)}` & `${with.toString(printer)}`")
                else -> Anything
            }

            else -> Anything
        }

        else -> Anything
    }

    override fun compare(ctx: Ctx, other: TypeComponent): TypeRelation {
        TODO("Not yet implemented")
    }
}