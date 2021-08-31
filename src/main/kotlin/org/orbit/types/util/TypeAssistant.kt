package org.orbit.types.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.types.components.Context
import org.orbit.types.typeactions.TypeAction
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

class TypeAssistant(private val context: Context) : KoinComponent {
    private val printer: Printer by inject()
    private val actions = mutableListOf<TypeAction>()

    fun perform(action: TypeAction) {
        actions.add(action)
        action.execute(context)
    }

    fun dump() : String {
        return """
            |${printer.apply("Type Assistant:", PrintableKey.Italics, PrintableKey.Bold)}
            |    ${actions.joinToString("\n\t", transform = { it.describe(printer) })}
        """.trimMargin()
    }
}