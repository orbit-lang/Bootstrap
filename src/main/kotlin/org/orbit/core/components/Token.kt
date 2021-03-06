package org.orbit.core.components

data class Token(
    val type: TokenType,
    val text: String,
    val position: SourcePosition
) {
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

