package org.orbit.core.components

import org.orbit.serial.Serial
import java.io.Serializable

abstract class TokenType(
	val identifier: String,
	val pattern: String,
	val ignoreWhitespace: Boolean,
	val isWhitespace: Boolean,
	val family: Family
) : Serializable {
	enum class Family : Serializable {
		Keyword, Id, Op, Enclosing, White, Comment, Num;
	}

	override fun equals(other: Any?): Boolean = when (other) {
		is TokenType -> other.identifier == identifier
		else -> false
	}
}