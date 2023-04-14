package org.orbit.backend.typesystem.components

import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Attribute(val name: String, val body: IAttributeExpression, val typeVariables: List<AnyType> = emptyList(), val effects: List<TypeEffect>) :
    IAttribute {
    override val id: String = "attribute $name"

    override fun invoke(env: IMutableTypeEnvironment) : AnyMetaType {
        return when (val result = body.evaluate(env)) {
            is Never -> result.panic()
            else -> {
                // If the premise `body` succeeds, we can now propagate our effects
                effects.forEach { it.invoke(env) }

                Always
            }
        }
    }

    // NOTE - This currently implies that substitution always proceeds L -> R,
    //  which might not be universally true
    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = typeVariables.filterIsInstance<TypeVar>()

    override fun getCanonicalName(): String = name

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Zero

    override fun substitute(substitution: Substitution): AnyType {
        val unsolved = getUnsolvedTypeVariables()

        if (unsolved.isEmpty()) return this

        return Attribute(
            name,
            body.substitute(substitution) as IAttributeExpression,
            typeVariables.substitute(substitution),
            effects.substitute(substitution) as List<TypeEffect>
        )
    }

    override fun prettyPrint(depth: Int): String {
        val printer = getKoinInstance<Printer>()
        val prettyName = printer.apply(name, PrintableKey.Italics, PrintableKey.Bold)
        val prettyTypeVars = typeVariables.joinToString(", ")

        return "$prettyName($prettyTypeVars)"
    }

    override fun toString(): String
        = prettyPrint()
}