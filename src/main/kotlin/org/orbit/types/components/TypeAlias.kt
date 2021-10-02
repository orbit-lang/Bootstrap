package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

data class TypeAlias(override val name: String, val targetType: Type) : VirtualType, TypeExpression {
    constructor(path: Path, targetType: Type) : this(path.toString(OrbitMangler), targetType)

    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol>
        get() = targetType.equalitySemantics

    override fun evaluate(context: Context): TypeProtocol = targetType

    override fun toString(printer: Printer): String {
        return "(${printer.apply(name, PrintableKey.Italics)} = ${targetType.toString(printer)})"
    }
}