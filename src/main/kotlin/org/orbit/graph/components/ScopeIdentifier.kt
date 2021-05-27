package org.orbit.graph.components

import org.json.JSONObject
import org.orbit.serial.Serial
import java.io.Serializable
import java.util.*

data class ScopeIdentifier(private val uuid: UUID) : Serial, Serializable {
	companion object {
		fun next() : ScopeIdentifier {
			return ScopeIdentifier(UUID.randomUUID())
		}
	}

	override fun describe(json: JSONObject) {
		json.put("scope.identifier", uuid.toString())
	}
}