package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.TupleTypeNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TupleTypeRule : ValueRule<TupleTypeNode> {
	override fun parse(context: Parser): ParseRule.Result {
		val collector = context.startCollecting()
		val start = context.expect(TokenTypes.LParen)
		val left = context.attempt(TypeExpressionRule)
			?: return ParseRule.Result.Failure.Rewind(collector)

		context.expect(TokenTypes.Comma)

		val right = context.attempt(TypeExpressionRule)
			?: return ParseRule.Result.Failure.Rewind(collector)

		val end = context.expect(TokenTypes.RParen)

		return +TupleTypeNode(start, end, left, right)
	}
}