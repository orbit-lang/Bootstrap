package org.orbit.core

abstract class TokenType(
	val identifier: String,
	val pattern: String,
	val ignoreWhitespace: Boolean,
	val isWhitespace: Boolean,
	val family: Family
) {
	enum class Family {
		Keyword, Id, Op, Enclosing, White, Comment, Num;
	}

	override fun equals(other: Any?): Boolean = when (other) {
		is TokenType -> other.identifier == identifier
		else -> false
	}
}