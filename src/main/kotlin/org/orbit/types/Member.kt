package org.orbit.types

import org.json.JSONObject
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser

data class Member(val name: String, val type: Type) : Serial {
    override fun describe(json: JSONObject) {
        json.put("member.name", name)
        json.put("member.type", Serialiser.serialise(type))
    }
}