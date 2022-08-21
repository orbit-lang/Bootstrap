package org.orbit.core.components

import java.io.Serializable

data class Token(
    val type: TokenType,
    val text: String,
    val position: SourcePosition
) : Serializable {
	companion object {
		val empty = Token(TokenTypes.Type, "", SourcePosition.unknown)
	}

	override fun equals(other: Any?): Boolean = when (other) {
		is Token -> {
			if (other.position == position && other.type != type) {
				throw Exception("COMPILER BUG: Different token types at same location: ${other.type} & ${type} @ $position")
			}

			other.type == type && other.position == position
		}
		else -> false
	}
}

