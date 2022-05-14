package org.orbit.types.next.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.components.TypeComponent
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.Printer

object RefinementErrorWriter : KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    @Throws
    fun missing(typeVariable: TypeComponent) {
        throw invocation.make<TypeSystem>("Unknown Type Variable ${typeVariable.toString(printer)} in Equality Constraint",
            SourcePosition.unknown
        )
    }

    @Throws
    fun doubleRefinement(typeVariable: TypeComponent) {
        throw invocation.make<TypeSystem>("Equality Constraints may only be applied to Type Variables. ${typeVariable.toString(
            printer
        )} is already a concrete type, possibly as the result of a previous constraint",
            SourcePosition.unknown
        )
    }

    @Throws
    fun cyclicConstraint(source: TypeComponent, target: TypeComponent) {
        throw invocation.make<TypeSystem>("Cyclic Equality Constraint detected between ${source.toString(printer)} and ${target.toString(
            printer
        )}",
            SourcePosition.unknown
        )
    }
}