package org.orbit.types

import org.json.JSONObject
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser

interface TypeProtocol : Serial {
    val name: String
    val equalitySemantics: Equality<out TypeProtocol>

    override fun describe(json: JSONObject) {
        json.put("type.meta", javaClass.simpleName)
        json.put("type.name", name)
    }
}

interface ValuePositionType : TypeProtocol

operator fun Entity.plus(name: String) : Property {
    return Property(name, this)
}