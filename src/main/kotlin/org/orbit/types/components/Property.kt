package org.orbit.types.components

import org.json.JSONObject
import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.RValueNode
import org.orbit.serial.Serialiser
import org.orbit.util.AnyPrintable
import org.orbit.util.PrintableKey
import org.orbit.util.Printer
import org.orbit.util.partialReverse

interface TypeEqualityUtil<T: TypeProtocol> {
    fun equal(context: ContextProtocol, equality: Equality<TypeProtocol, TypeProtocol>, a: T, b: T) : Boolean
}

data class Property(
    override val name: String,
    val type: TypeProtocol,
    val defaultValue: ExpressionNode? = null
) : TypeProtocol, AnyPrintable {
    companion object : TypeEqualityUtil<Property> {
        override fun equal(context: ContextProtocol, equality: Equality<TypeProtocol, TypeProtocol>, a: Property, b: Property): Boolean {
            return a.name == b.name && equality.isSatisfied(context, a.type, b.type)
        }
    }

    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = type.equalitySemantics
    override val kind: TypeKind = NullaryType

    // TODO - default values
    constructor(pair: Pair<PairNode, TypeProtocol>)
        : this(pair.first.identifierNode.identifier, pair.second)

    override fun describe(json: JSONObject) {
        json.put("member.name", name)
        json.put("member.type", Serialiser.serialise(type))
    }

    override fun toString(printer: Printer): String {
        return "(${printer.apply(name, PrintableKey.Italics)} => ${type.toString(printer)})"
    }
}

fun Collection<Property>.drawContracts() : List<PropertyContract> {
    return map(::PropertyContract)
}

fun Entity.drawPropertyContracts() : List<PropertyContract> {
    return properties.drawContracts()
}

fun TypeProtocol.executeContract(context: ContextProtocol, contract: TypeContract) : Boolean {
    return contract.isSatisfiedBy(context, this)
}

fun TypeProtocol.executeContracts(context: ContextProtocol, contracts: Collection<TypeContract>) : Boolean {
    return contracts.map(partialReverse(::executeContract, context)).fold(true, Boolean::and)
}