package org.orbit.types

import org.json.JSONObject
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser

interface Type : Serial {
    val name: String
    val members: List<Member>
    val behaviours: List<Behaviour>

    override fun describe(json: JSONObject) {
        json.put("type.meta", javaClass.simpleName)
        json.put("type.name", name)

        val membersJson = members.map { Serialiser.serialise(it) }

        json.put("members", membersJson)
    }
}

operator fun Type.plus(name: String) : Member {
    return Member(name, this)
}