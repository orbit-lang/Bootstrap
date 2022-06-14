package org.orbit.core.components

import java.util.function.Function

/**
 * This abstraction allows us to create new TokenTypes on the fly without having to declare them as part of the core
 * syntax. This enables us to use previously reserved characters (e.g. `.`, `<`, etc) as user-defined operator symbols
 * without the Lexer greedily classifying such strings as syntactic symbols before we get a chance to see them in
 * operator definitions.
 *
 * For example, before this commit, Dot (`.`) was reserved as a special syntactic TokenType, meaning the following
 * always caused a syntax error:
 *
 * ``infix operator range `...` by ::Range``
 *
 * This is now possible because Dot is always considered part of the `OperatorSymbol` Lexical rule instead.
 */
interface ITokenType {
	fun getPredicate() : (Token) -> Boolean
}

data class VirtualTokenType(private val text: String) : ITokenType {
	override fun getPredicate(): (Token) -> Boolean = { token ->
		token.text == text
	}

	operator fun invoke(token: Token) : Boolean
		= getPredicate().invoke(token)
}

abstract class TokenType(
	val identifier: String,
	val pattern: String,
	val ignoreWhitespace: Boolean,
	val isWhitespace: Boolean,
	val family: Family
) : ITokenType {
	sealed class Family(val name: String) {
		object Keyword : Family("Keyword")
		object Id : Family("Id")
		object Op : Family("Op")
		object Enclosing : Family("Enclosing")
		object White : Family("White")
		object Comment : Family("Comment")
		object Num : Family("Num")
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

	override fun getPredicate(): (Token) -> Boolean = { token ->
		token.type == this
	}

	override fun equals(other: Any?): Boolean = when (other) {
		is TokenType -> other.identifier == identifier
		else -> false
	}
}
