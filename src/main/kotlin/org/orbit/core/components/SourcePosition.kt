package org.orbit.core.components

import com.google.gson.*
import java.io.Serializable
import java.lang.reflect.Type

data class SourcePosition(
	val line: Int,
	val character: Int,
	val absolute: Int = 0
) : Serializable {
	companion object : JsonSerializer<SourcePosition>, JsonDeserializer<SourcePosition> {
		val unknown = SourcePosition(-1, -1, -1)

		override fun serialize(src: SourcePosition, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
			return JsonObject().apply {
				addProperty("line", src.line)
				addProperty("character", src.character)
				addProperty("absolute", src.absolute)
			}
		}

		override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SourcePosition {
			val json = json.asJsonObject ?: return unknown
			val line = json["line"].asInt
			val character = json["character"].asInt
			val absolute = json["absolute"].asInt

			return SourcePosition(line, character, absolute)
		}
	}

	fun moveLine(by: Int) : SourcePosition {
		// Moving on by > 0 lines resets character back to 0
		return SourcePosition(line + by, 0, absolute + by)
	}

	fun moveCharacter(by: Int) : SourcePosition {
		return SourcePosition(line, character + by, by + absolute)
	}

	override fun equals(other: Any?): Boolean = when (other) {
		is SourcePosition -> other.line == line && other.character == character
		else -> false
	}

	override fun toString(): String {
		return "(line: ${line + 1}, offset: ${character + 1})"
	}
}