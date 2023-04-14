package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.utils.AnyArrow

data class ConstrainedArrow(val arrow: AnyArrow, val constraints: List<IAttribute>, val fallback: AnyType? = null, override val effects: List<Effect> = emptyList(), val referencedVariadicIndices: List<Int> = emptyList()) : IArrow<ConstrainedArrow>,
    IConstructableType<ConstrainedArrow> {
    override val id: String = "$arrow + ${constraints.joinToString(", ")}"

    override fun isSpecialised(): Boolean = false

    override fun getDomain(): List<AnyType>
        = arrow.getDomain()

    override fun getCodomain(): AnyType
        = arrow.getCodomain()

    override fun getCardinality(): ITypeCardinality
        = arrow.getCardinality()

    override fun curry(): IArrow<*>
        = arrow.curry()

    override fun never(args: List<AnyType>): Never
        = arrow.never(args)

    override fun substitute(substitution: Substitution): AnyType = when (fallback) {
        null -> ConstrainedArrow(
            arrow.substitute(substitution) as AnyArrow,
            constraints.substitute(substitution) as List<Attribute>,
            null,
            effects.substitute(substitution) as List<Effect>,
            referencedVariadicIndices
        )
        else -> ConstrainedArrow(
            arrow.substitute(substitution) as AnyArrow,
            constraints.substitute(substitution) as List<Attribute>,
            fallback.substitute(substitution),
            effects.substitute(substitution) as List<Effect>,
            referencedVariadicIndices
        )
    }

    override fun extendDomain(with: List<AnyType>): AnyArrow
        = ConstrainedArrow(arrow.extendDomain(with), constraints, fallback, effects, referencedVariadicIndices)

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)
        val prettyDomain = arrow.getDomain().joinToString(", ")
        val prettyArrow = "($prettyDomain) => ${arrow.getCodomain()}"

        return when (constraints.isEmpty()) {
            true -> "$indent$prettyArrow"
            else -> {
                val pretty = constraints.joinToString(" & ")

                "$indent$prettyArrow where $pretty"
            }
        }
    }

    override fun toString(): String
        = prettyPrint()
}