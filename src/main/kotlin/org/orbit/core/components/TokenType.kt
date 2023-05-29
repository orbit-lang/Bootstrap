package org.orbit.core.components

abstract class TokenType(
	val identifier: String,
	val pattern: String,
	val ignoreWhitespace: Boolean,
	val isWhitespace: Boolean,
	val family: Family
) {
	sealed class Family(val name: String) {
		object Keyword : Family("Keyword")
		object Id : Family("Id")
		object Op : Family("Op")
		object Enclosing : Family("Enclosing")
		object White : Family("White")
		object Comment : Family("Comment")
		object Num : Family("Num")
		object Text : Family("Text")
		object Kind : Family("Kind")
		object CompileTime : Family("CompileTime")
		data class Either(val left: Family, val right: Family) : Family("Either<${left.name}, ${right.name}>")

		operator fun plus(other: Family) : Either
			= Either(this, other)

		fun contains(family: Family) : Boolean = when (this) {
			is Either -> left.contains(family) || right.contains(family)
			else -> name == family.name
		}
	}

	override fun equals(other: Any?): Boolean = when (other) {
		is TokenType -> other.identifier == identifier
		else -> false
	}

	override fun toString(): String
		= identifier
}
