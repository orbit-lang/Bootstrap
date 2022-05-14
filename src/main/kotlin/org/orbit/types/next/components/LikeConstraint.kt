package org.orbit.types.next.components

import org.orbit.core.components.SourcePosition
import org.orbit.types.next.inference.TypeReference
import org.orbit.types.next.phase.TypeSystem
import org.orbit.types.next.utils.ConstraintErrorWriter
import org.orbit.util.Invocation
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class LikeConstraint(val source: TypeComponent, val trait: TypeComponent) : ITypeConstraint<ITrait> {
    override val fullyQualifiedName: String = "${source.fullyQualifiedName} : ${trait.fullyQualifiedName}"

    override fun check(ctx: Ctx) = when (source) {
        is TypeVariable -> {
            val invocation = getKoinInstance<Invocation>()
            val printer = getKoinInstance<Printer>()

            throw invocation.make<TypeSystem>("Constraint ${source.toString(printer)} : ${trait.toString(printer)}, unrefined for Type Variable ${source.toString(printer)}",
                SourcePosition.unknown
            )
        }

        else -> when (trait) {
            is ITrait -> when (StructuralEq.eq(ctx, TypeReference(trait.fullyQualifiedName), source)) {
                true -> {}
                else -> ConstraintErrorWriter.brokenConstraint(this)
            }
            else -> {
                val invocation = getKoinInstance<Invocation>()
                val printer = getKoinInstance<Printer>()
                throw invocation.make<TypeSystem>("Cannot check for conformance against non-Trait ${trait.toString(printer)}",
                    SourcePosition.unknown
                )
            }
        }
    }

    override fun substitute(typeVariable: TypeVariable, type: TypeComponent): ITypeConstraint<ITrait> {
        return if (source is TypeVariable && source.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            LikeConstraint(type, trait)
        } else if (trait is TypeVariable && trait.fullyQualifiedName == typeVariable.fullyQualifiedName) {
            LikeConstraint(source, type)
        } else this
    }

    override fun getRefinement(): ContextualRefinement
        = ConformanceRefinement(source, trait as ITrait)
}