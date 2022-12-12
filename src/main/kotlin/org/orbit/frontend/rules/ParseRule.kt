package org.orbit.frontend.rules

import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.INode
import org.orbit.frontend.phase.Parser

interface ParseRule<N: INode> {
	interface Result {
		data class Success<N: INode>(val node: N) : Result
		sealed class Failure : Result {
			object Abort : Result
			data class Rewind(val tokens: List<Token> = emptyList()) : Result {
				constructor(collector: Parser.TokenCollector) : this(collector.getCollectedTokens())
			}
			data class Throw(val message: String, val position: SourcePosition) : Result {
				constructor(message: String, token: Token) : this(message, token.position)
				constructor(message: String, collector: Parser.TokenCollector) : this(message, collector.getCollectedTokens().last())
			}
		}

		fun <R: Result> unwrap() : R? {
			return this as? R
		}

		fun <N: INode> asSuccessOrNull() : Success<N>? {
			return unwrap()
		}
	}

	fun parse(context: Parser) : Result

	fun execute(input: Parser): Result {
		return parse(input)
	}
}

fun <N: INode> ParseRule<N>.toBlockRule() : BlockRule
	= BlockRule(this)

fun <N: INode> ParseRule<N>.toDelimitedRule(openingType: TokenType = TokenTypes.LParen, closingType: TokenType = TokenTypes.RParen, delimiter: TokenType = TokenTypes.Comma) : DelimitedRule<N>
	= DelimitedRule(openingType, closingType, this, delimiter)
