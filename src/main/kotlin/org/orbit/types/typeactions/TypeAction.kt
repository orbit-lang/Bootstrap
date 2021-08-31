package org.orbit.types.typeactions

import org.orbit.types.components.Context
import org.orbit.util.Printer

interface TypeAction {
    fun execute(context: Context)
    fun describe(printer: Printer) : String
}