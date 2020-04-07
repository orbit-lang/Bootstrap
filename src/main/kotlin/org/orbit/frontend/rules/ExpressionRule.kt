package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.Parser
import org.orbit.frontend.ParseRule
import org.orbit.frontend.TokenTypes

interface ValueRule<E: ExpressionNode> : ParseRule<E>

class ExpressionRule(private vararg val valueRules: ValueRule<*>) : ParseRule<ExpressionNode> {
	override fun parse(context: Parser) : ExpressionNode {
		val start = context.peek()
		var isGrouped = false

		if (start.type == TokenTypes.LParen) {
			// Grouped expression, e.g. `(2 + 2)`
			isGrouped = true
			context.consume()
		}
	
		val expr = context.attemptAny(*valueRules) as? ExpressionNode
			?: throw Exception("TODO")

		if (isGrouped) context.expect(TokenTypes.RParen)

		return expr
	}
}
