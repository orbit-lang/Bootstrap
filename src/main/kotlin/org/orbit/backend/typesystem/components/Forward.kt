package org.orbit.backend.typesystem.components

import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Forward(val name: String) : IType {
    override val id: String = name

    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Zero
    override fun substitute(substitution: Substitution): AnyType = this
    override fun flatten(from: AnyType, env: ITypeEnvironment): AnyType
        = env.getTypeOrNull(name)
            ?.component
            ?.flatten(from, env)
            ?: throw Exception("HERE: $name")

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)
        val printer = getKoinInstance<Printer>()
        val pretty = printer.apply(name, PrintableKey.Bold)

        return "$indent$pretty"
    }

    override fun toString(): String
        = prettyPrint()
}