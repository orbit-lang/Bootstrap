package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.TupleLiteralNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TupleLiteralRule : ValueRule<TupleLiteralNode> {
	override fun parse(context: Parser): ParseRule.Result {
		context.mark()
		val start = context.expect(TokenTypes.LParen)
		val left = context.attempt(ExpressionRule.defaultValue)
			?: return ParseRule.Result.Failure.Rewind(context.end())

		context.expect(TokenTypes.Comma)

		val right = context.attempt(ExpressionRule.defaultValue)
			?: return ParseRule.Result.Failure.Rewind(context.end())

		val end = context.expect(TokenTypes.RParen)

		return +TupleLiteralNode(start, end, Pair(left, right))
	}
}