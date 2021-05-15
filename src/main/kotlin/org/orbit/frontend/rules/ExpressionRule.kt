package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.Parser
import org.orbit.frontend.ParseRule
import org.orbit.frontend.TokenTypes

interface ValueRule<E: ExpressionNode> : ParseRule<E>

class ExpressionRule(vararg val valueRules: ValueRule<*>) : ParseRule<ExpressionNode> {
	companion object {
		val defaultValue = ExpressionRule(
			UnaryExpressionRule,
			ConstructorRule(),
			LiteralRule()
		)
	}

	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.peek()
		var isGrouped = false

		if (start.type == TokenTypes.LParen) {
			// Grouped expression, e.g. `(2 + 2)`
			isGrouped = true
			context.consume()
		}
	
		val expr = context.attemptAny(*valueRules) as? ExpressionNode
			?: TODO("@ExpressionRule:30")

		val next = context.peek()

		if (isGrouped && next.type == TokenTypes.RParen) {
			context.consume()
			return parseTrailing(context, expr)
		}

		if (next.type == TokenTypes.Operator) {
			try {
				val partialExpressionRule = PartialExpressionRule(expr)

				return partialExpressionRule.execute(context)
			} finally {
				if (isGrouped) context.expect(TokenTypes.RParen)
			}
		} else if (next.type == TokenTypes.Dot) {
			try {
				val callRule = PartialCallRule(expr)

				return callRule.execute(context)
			} finally {
				if (isGrouped) context.expect(TokenTypes.RParen)
			}
		}

		if (isGrouped) context.expect(TokenTypes.RParen)

		return ParseRule.Result.Success(expr)
	}
}
