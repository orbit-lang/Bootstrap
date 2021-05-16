package org.orbit.frontend.rules

import org.orbit.core.Token
import org.orbit.core.nodes.Node
import org.orbit.frontend.phase.Parser

interface ParseRule<N: Node> {
	interface Result {
		data class Success<N: Node>(val node: N) : Result
		sealed class Failure : Result {
			object Abort : Result
			data class Rewind(val tokens: List<Token> = emptyList()) : Result
		}

		fun <R: Result> unwrap() : R? {
			return this as? R
		}

		fun <N: Node> asSuccessOrNull() : Success<N>? {
			return unwrap()
		}
	}

	fun parse(context: Parser) : Result

	fun execute(input: Parser): Result {
		return parse(input)
	}
}