package org.orbit.core

data class SourcePosition(
	val line: Int,
	val character: Int) {

	fun moveLine(by: Int) : SourcePosition {
		// Moving on by > 0 lines resets character back to 0
		return SourcePosition(line + by, 0)
	}

	fun moveCharacter(by: Int) : SourcePosition {
		return SourcePosition(line, character + by)
	}

	override fun equals(other: Any?): Boolean = when (other) {
		is SourcePosition -> other.line == line && other.character == character
		else -> false
	}

	override fun toString(): String {
		return "(line: $line, offset: $character)"
	}
}

abstract class TokenType(
	val identifier: String,
	val pattern: String,
	val ignoreWhitespace: Boolean,
	val isWhitespace: Boolean
) {
	override fun equals(other: Any?): Boolean = when (other) {
		is TokenType -> other.identifier == identifier
		else -> false
	}
}

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

interface TokenTypeProvider {
	fun getTokenTypes() : List<TokenType>
}

interface SourceProvider {
	fun getSource() : String
}
