package org.orbit.types

import org.json.JSONObject
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser

data class Member(
    override val name: String,
    val type: Type
) : Type {
    override val members: List<Member> = emptyList()
    override val behaviours: List<Behaviour> = emptyList()

    override fun describe(json: JSONObject) {
        json.put("member.name", name)
        json.put("member.type", Serialiser.serialise(type))
    }
}