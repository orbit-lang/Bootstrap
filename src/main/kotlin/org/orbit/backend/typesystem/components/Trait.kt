package org.orbit.backend.typesystem.components

import org.orbit.core.OrbitMangler
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Trait(override val id: String, val properties: List<Property>, val signatures: List<Signature>) : Entity<Trait>, TraitMember {
    override fun substitute(substitution: Substitution): Trait
        = Trait(id, properties.map { it.substitute(substitution) }, signatures.map { it.substitute(substitution) })

    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Infinite

    override fun equals(other: Any?): Boolean = when (other) {
        is Trait -> other.id == id
        else -> false
    }

    private fun isImplementedBy(struct: Struct, env: ITypeEnvironment) : Boolean {
        val projections = GlobalEnvironment.getProjectedTags(struct)

        return projections.any { it.component.target.id == id }
    }

    fun isImplementedBy(type: AnyType, env: ITypeEnvironment) : Boolean {
        val type = type.flatten(type, env)
        if (type is Trait) return false // TODO - Work out the rules for Trait : Trait
        if (type is Struct) return isImplementedBy(type, env)

        val projections = env.getProjections(type) + when (env) {
            is ProjectionEnvironment -> listOf(ContextualDeclaration(env.getCurrentContext(), env.projection))
            else -> emptyList()
        }

        return projections.any { it.component.target.id == id }
    }

    operator fun plus(other: Trait) : Trait
        = Trait("$id*${other.id}", properties + other.properties, signatures + other.signatures)

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)
        val printer = getKoinInstance<Printer>()
        val path = OrbitMangler.unmangle(id)
        val simpleName = path.last()

        return "$indent${printer.apply(simpleName, PrintableKey.Bold)}"
    }

    override fun toString(): String = prettyPrint()
}