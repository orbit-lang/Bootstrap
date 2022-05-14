package org.orbit.types.next.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.SourcePosition
import org.orbit.types.next.components.ITypeConstraint
import org.orbit.types.next.phase.TypeSystem
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

object ConstraintErrorWriter : KoinComponent {
    private val invocation: Invocation by inject()
    private val printer: Printer by inject()

    @Throws
    fun brokenConstraint(constraint: ITypeConstraint<*>) {
        throw invocation.make<TypeSystem>("Contextual Constraint broken: ${
            printer.apply(constraint.fullyQualifiedName,
            PrintableKey.Bold,
            PrintableKey.Italics
        )}", SourcePosition.unknown
        )
    }
}