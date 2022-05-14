package org.orbit.types.next.components

import org.koin.core.component.KoinComponent
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.phase.TypeSystem
import org.orbit.types.next.utils.ConstraintErrorWriter
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class SameConstraint(val source: TypeComponent, val target: TypeComponent) : ITypeConstraint<TypeComponent>,
    KoinComponent {
    override val fullyQualifiedName: String = "${source.fullyQualifiedName} = ${target.fullyQualifiedName}"

    override fun check(ctx: Ctx) = when (source) {
        is TypeVariable -> {
            val invocation = getKoinInstance<Invocation>()
            val printer = getKoinInstance<Printer>()

            throw invocation.make<TypeSystem>("Constraint ${source.toString(printer)} = ${target.toString(printer)}, unrefined for Type Variable ${source.toString(printer)}",
                SourcePosition.unknown
            )
        }

        else -> when (NominalEq.eq(ctx, source, target)) {
            true -> {}
            else -> ConstraintErrorWriter.brokenConstraint(this)
        }
    }

    override fun substitute(typeVariable: TypeVariable, type: TypeComponent): ITypeConstraint<TypeComponent> {
        return if (source is TypeVariable && source.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            SameConstraint(type, target)
        } else if (target is TypeVariable && target.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            SameConstraint(source, type)
        } else this
    }

    override fun getRefinement(): ContextualRefinement
        = EqualityRefinement(source, target)
}