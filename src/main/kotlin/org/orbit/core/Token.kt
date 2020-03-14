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
}

abstract class TokenType(
	val identifier: String,
	val pattern: String,
	val ignoreWhitespace: Boolean,
	val isWhitespace: Boolean
)

data class Token(
	val type: TokenType,
	val text: String,
	val position: SourcePosition
)

interface TokenTypeProvider {
	fun getTokenTypes() : Array<TokenType>
}

interface SourceProvider {
	fun getSource() : String
}
