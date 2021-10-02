package org.orbit.types.components

import org.orbit.util.Printer

data class Parameter(override val name: String, val type: TypeProtocol) : TypeProtocol {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = type.equalitySemantics

    override fun toString(printer: Printer): String {
        return "${name}: ${type.toString(printer)}"
    }
}