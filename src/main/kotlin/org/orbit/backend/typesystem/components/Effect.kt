package org.orbit.backend.typesystem.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Effect(val name: String, val takes: List<AnyType>, val gives: AnyType) : IArrow<Effect>, IStructuralType {
    companion object {
        val die = Effect("Die", emptyList(), Always)
    }

    constructor(path: Path, takes: List<AnyType>, gives: AnyType) : this(path.toString(OrbitMangler), takes, gives)

    override val id: String = name
    override val effects: List<Effect> = emptyList()
    override val members: List<Pair<String, AnyType>> = takes.map { Pair("", it) }

    override fun curry(): IArrow<*> = this

    override fun never(args: List<AnyType>): Never {
        TODO("Not yet implemented")
    }

    override fun getDomain(): List<AnyType>
        = takes

    override fun getCodomain(): AnyType
        = gives

    // TODO - Effects probably have the Cardinality of the sum of all their parameters
    override fun getCardinality(): ITypeCardinality
        = ITypeCardinality.Infinite

    override fun substitute(substitution: Substitution): AnyType
        = Effect(name, takes.substitute(substitution), gives.substitute(substitution))

    override fun equals(other: Any?): Boolean = when (other) {
        is Effect -> other.name == name && other.takes == takes && other.gives == gives
        else -> false
    }

    override fun prettyPrint(depth: Int): String {
        val printer = getKoinInstance<Printer>()

        return printer.apply(name, PrintableKey.Bold, PrintableKey.Italics)
    }

    override fun toString(): String
        = prettyPrint()
}