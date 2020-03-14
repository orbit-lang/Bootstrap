package org.orbit.frontend

import org.orbit.core.TokenType
import org.orbit.core.Token
import org.orbit.core.nodes.*
import org.orbit.core.Phase
import org.orbit.core.Warning

interface ParseRule<N: Node> {
	fun parse(context: Parser) : N
}

data class ParseResult(val ast: Node, val warnings: List<Warning>)

class Parser(private val topLevelParseRule: ParseRule<*>)
	: Phase<Array<Token>, ParseResult> {

	private var warnings = mutableListOf<Warning>()
	
	sealed class Errors {
		object NoMoreTokens : Exception("There are no more tokens left to parse")
		object NoParseRules : Exception("Parse rules were not provided")
		data class UnexpectedToken(val type: TokenType) : Exception("Unexpected token: ${type.identifier}")
	}
	
	private var tokens: MutableList<Token> = mutableListOf()

	var hasMore: Boolean = false
		get() = tokens.isNotEmpty()

	fun warn(warning: Warning) {
		warnings.add(warning)
	}
	
	fun peek() : Token {
		return tokens.getOrNull(0) ?: throw Parser.Errors.NoMoreTokens
	}

	fun consume() : Token {
		if (!hasMore) throw Parser.Errors.NoMoreTokens
		
		return tokens.removeAt(0)
	}

	fun expect(type: TokenType) : Token {
		if (!hasMore) throw Parser.Errors.NoMoreTokens
		val next = peek()
		
		return when (next.type) {
			type -> consume()
			else -> throw Parser.Errors.UnexpectedToken(next.type)
		}
	}

	fun expectAny(vararg types: TokenType, consumes: Boolean) : Token {
		if (!hasMore) throw Parser.Errors.NoMoreTokens
		val next = peek()

		if (next.type in types) {
			if (consumes) {
				return consume()
			}

			return next
		}

		throw Parser.Errors.UnexpectedToken(next.type)
	}

	fun rewind(consumed: List<Token>) {
		tokens.addAll(0, consumed)
	}

	fun <N: Node> attempt(rule: ParseRule<N>, rethrow: Boolean = false) : N? {
		val backup = tokens

		try {
			return rule.parse(this)
		} catch (ex: Exception) {
			// We don't care why this failed, only that it did
			if (rethrow) throw ex
			
			tokens = backup
		}

		return null
	}

	// This is about as close to vararg generics as we can get
	fun <T: Node, U: Node> attemptAny(of1: ParseRule<T>, of2: ParseRule<U>) : Pair<T?, U?>? {
		val result1 = attempt(of1)

		if (result1 != null) return Pair(result1, null)

		val result2 = attempt(of2)

		if (result2 != null) return Pair(null, result2)

		return null
	}
	
	fun attemptAny(vararg of: ParseRule<*>, propagate: Boolean = false) : Node? {
		val backup = tokens

		for (rule in of) {
			try {
				return rule.parse(this)
			} catch (ex: Exception) {
				tokens = backup

				if (propagate) {
					throw ex
				}
			}
		}

		return null
	}

	override fun execute(input: Array<Token>) : ParseResult {
		if (input.isEmpty()) throw Parser.Errors.NoMoreTokens
		
		tokens = input.toMutableList()
		
		val ast = topLevelParseRule.parse(this)

		return ParseResult(ast, warnings)
	}
}