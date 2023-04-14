package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Signature(val receiver: AnyType, val name: String, val parameters: List<AnyType>, val returns: AnyType, val isInstanceSignature: Boolean, override val effects: List<Effect> = emptyList()) : IArrow<Signature>,
    Trait.Member {
    override val id: String get() {
        val pParams = parameters.joinToString(", ") { it.id }

        return "$receiver.$name($pParams)($returns)"
    }

    override fun getUnsolvedTypeVariables(): List<TypeVar>
        = (receiver.getUnsolvedTypeVariables() + returns.getUnsolvedTypeVariables() + parameters.flatMap { it.getUnsolvedTypeVariables() })
            .distinct()

    override fun getDomain(): List<AnyType> = toArrow().getDomain()
    override fun getCodomain(): AnyType = toArrow().getCodomain()

    override fun curry(): IArrow<*> = toArrow().curry()
    override fun never(args: List<AnyType>): Never = toArrow().never(args)

    override fun getCardinality(): ITypeCardinality
        = returns.getCardinality()

    private fun toInstanceArrow() = when (parameters.count()) {
        0 -> Arrow1(receiver, returns, effects)
        1 -> Arrow2(receiver, parameters[0], returns, effects)
        2 -> Arrow3(receiver, parameters[0], parameters[1], returns, effects)
        else -> TODO("3+-ary instance Arrows")
    }

    fun toStaticArrow(): AnyArrow = parameters.arrowOf(returns, effects)

    fun toArrow() : AnyArrow = when (isInstanceSignature) {
        true -> toInstanceArrow()
        else -> toStaticArrow()
    }

    override fun substitute(substitution: Substitution): Signature
        = Signature(
        receiver.substitute(substitution),
        name,
        parameters.map { it.substitute(substitution) },
        returns.substitute(substitution),
        isInstanceSignature
    )

    override fun equals(other: Any?): Boolean = when (other) {
        is Signature -> other.name == name && other.receiver == receiver && other.parameters == parameters && other.returns == returns
        else -> false
    }

    override fun prettyPrint(depth: Int): String {
        val indent = "\t".repeat(depth)
        val prettyParams = parameters.joinToString(", ") { it.prettyPrint(0) }
        val printer = getKoinInstance<Printer>()
        val prettyName = printer.apply(name, PrintableKey.Italics)

        return "$indent(${receiver.prettyPrint(0)}) $prettyName ($prettyParams) (${returns.prettyPrint(0)})"
    }

    override fun toString(): String = prettyPrint()
}