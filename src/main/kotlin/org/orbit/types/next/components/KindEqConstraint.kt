package org.orbit.types.next.components

import org.orbit.types.next.utils.ConstraintErrorWriter

data class KindEqConstraint(val source: TypeComponent, val target: TypeComponent) : ITypeConstraint<TypeComponent> {
    override val fullyQualifiedName: String = "${source.fullyQualifiedName} ^ ${target.fullyQualifiedName}"

    override fun check(ctx: Ctx) {
        val a = when (source) {
            is Kind -> source
            else -> source.kind
        }

        val b = when (target) {
            is Kind -> target
            else -> target.kind
        }

        if (!AnyEq.eq(ctx, a, b)) {
            ConstraintErrorWriter.brokenConstraint(KindEqConstraint(a, b))
        }
    }

    override fun getRefinement(): ContextualRefinement {
        TODO("Not yet implemented")
    }

    override fun substitute(typeVariable: TypeVariable, type: TypeComponent): ITypeConstraint<TypeComponent> {
        return if (source is TypeVariable && source.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            KindEqConstraint(type, target)
        } else if (target is TypeVariable && target.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            KindEqConstraint(source, type)
        } else this
    }
}