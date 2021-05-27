package org.orbit.types.components

import org.json.JSONObject
import org.orbit.serial.Serial
import java.io.Serializable

interface TypeProtocol : Serial, Serializable {
    val name: String
    val equalitySemantics: Equality<out TypeProtocol>

    fun isSatisfied(context: Context, type: TypeProtocol) : Boolean {
        return (equalitySemantics as AnyEquality).isSatisfied(context, this, type)
    }

    override fun describe(json: JSONObject) {
        json.put("type.meta", javaClass.simpleName)
        json.put("type.name", name)
    }
}

typealias TypeProtocolPair = Pair<TypeProtocol, TypeProtocol>

fun TypeProtocolPair.isSatisfied(context: Context) : Boolean {
    return first.isSatisfied(context, second)
}

interface ValuePositionType : TypeProtocol

operator fun Entity.plus(name: String) : Property {
    return Property(name, this)
}