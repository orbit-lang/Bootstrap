package org.orbit.types.components

import org.json.JSONObject
import org.orbit.serial.Serialiser
import org.orbit.util.partialAlt

interface TypeEqualityUtil<T: TypeProtocol> {
    fun equal(context: Context, equality: Equality<TypeProtocol>, a: T, b: T) : Boolean
}

data class Property(
    override val name: String,
    val type: TypeProtocol
) : TypeProtocol {
    override val equalitySemantics: Equality<out TypeProtocol> = type.equalitySemantics

    companion object : TypeEqualityUtil<Property> {
        override fun equal(context: Context, equality: Equality<TypeProtocol>, a: Property, b: Property): Boolean {
            return a.name == b.name && equality.isSatisfied(context, a.type, b.type)
        }
    }

    override fun describe(json: JSONObject) {
        json.put("member.name", name)
        json.put("member.type", Serialiser.serialise(type))
    }
}

fun Collection<Property>.drawContracts() : List<PropertyContract> {
    return map(::PropertyContract)
}

fun Entity.drawPropertyContracts() : List<PropertyContract> {
    return properties.drawContracts()
}

fun TypeProtocol.executeContract(context: Context, contract: TypeContract) : Boolean {
    return contract.isSatisfiedBy(context, this)
}

fun TypeProtocol.executeContracts(context: Context, contracts: Collection<TypeContract>) : Boolean {
    return contracts.map(partialAlt(::executeContract, context)).fold(true, Boolean::and)
}