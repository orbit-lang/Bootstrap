package org.orbit.backend.typesystem.components

import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Property(val name: String, val type: AnyType) : AnyType, TraitMember {
    override val id: String = "$name: $type"

    override fun getCardinality(): ITypeCardinality
        = type.getCardinality()

    override fun substitute(substitution: Substitution): Property =
        Property(name, type.substitute(substitution))

    override fun equals(other: Any?): Boolean = when (other) {
        is Property -> name == other.name && type == other.type
        else -> false
    }

    override fun prettyPrint(depth: Int): String {
        val printer = getKoinInstance<Printer>()
        val prettyName = printer.apply(name, PrintableKey.Italics)

        return "$prettyName: $type"
    }

    override fun toString(): String = prettyPrint()
}