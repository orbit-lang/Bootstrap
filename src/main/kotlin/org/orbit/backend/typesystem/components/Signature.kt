package org.orbit.backend.typesystem.components

import org.orbit.backend.typesystem.utils.AnyArrow
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.getKoinInstance

data class Signature(val receiver: AnyType, val name: String, val parameters: List<AnyType>, val returns: AnyType, val isInstanceSignature: Boolean, override val effects: List<Effect> = emptyList(), val isVirtual: Boolean = false) : IArrow<Signature>,
    TraitMember {
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

    override fun substitute(substitution: Substitution): Signature {
        val nReceiver = when (receiver) {
            substitution.old -> substitution.new
            else -> receiver
        }

        val nReturns = when (returns) {
            substitution.old -> substitution.new
            else -> returns
        }

        val nParameters = parameters.map {
            when (it) {
                substitution.old -> substitution.new
                else -> it
            }
        }

        return Signature(nReceiver.substitute(substitution), name, nParameters, nReturns, isInstanceSignature, effects.substitute(substitution) as List<Effect>, isVirtual)
    }

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

fun Signature.substituteReceiver(old: AnyType, new: AnyType) : Signature
    = Signature(receiver.substitute(old, new), name, parameters, returns, isInstanceSignature, effects, isVirtual)

fun Signature.substituteParameters(old: AnyType, new: AnyType) : Signature
    = Signature(receiver, name, parameters.substitute(Substitution(old, new)), returns, isInstanceSignature, effects, isVirtual)

fun Signature.substituteReturns(old: AnyType, new: AnyType) : Signature
    = Signature(receiver, name, parameters, returns.substitute(old, new), isInstanceSignature, effects, isVirtual)

fun AnyType.substitute(old: AnyType, new: AnyType) : AnyType
    = substitute(Substitution(old, new))