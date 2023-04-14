package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.intrinsics.OrbCoreTypes
import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

object Unit : Entity<Unit> {
    override val id: String = "Unit"

    override fun getPath(): Path
        = OrbitMangler.unmangle("Orb::Core::Types::Unit")

    override fun getCardinality(): ITypeCardinality = ITypeCardinality.Mono
    override fun substitute(substitution: Substitution): Unit = this
    override fun equals(other: Any?): Boolean = when (other) {
        is Unit -> true
        else -> false
    }

    override fun prettyPrint(depth: Int): String {
        val printer = getKoinInstance<Printer>()

        return printer.apply(OrbCoreTypes.unitType.getCanonicalName(), PrintableKey.Bold)
    }

    override fun toString(): String = prettyPrint()
}