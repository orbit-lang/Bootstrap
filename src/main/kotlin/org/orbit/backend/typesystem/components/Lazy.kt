package org.orbit.backend.typesystem.components

import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Lazy<T: AnyType>(val name: String, val type: () -> T) : IType {
    override val id: String = "⎡$name⎦"

    override fun getCardinality(): ITypeCardinality
        = type().getCardinality()

    override fun substitute(substitution: Substitution): AnyType
        = Lazy(name) { type().substitute(substitution) }

    override fun prettyPrint(depth: Int): String {
        val printer = getKoinInstance<Printer>()
        val pretty = printer.apply(name, PrintableKey.Bold)

        return "${"\t".repeat(depth)}⎡$pretty⎦"
    }

    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType = type().flatten(from, env)

    override fun toString(): String
        = prettyPrint()
}