package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.Parser
import org.orbit.frontend.ParseRule
import org.orbit.frontend.TokenTypes

object ReturnRule : ParseRule<ReturnStatementNode> {
	override fun parse(context: Parser) : ReturnStatementNode {
		val start = context.expect(TokenTypes.Return)
		val next = context.peek()

		if (next.type == TokenTypes.RBrace) {
			// This is an empty return, `return`,
			// syntactic sugar for `return Unit`
			return ReturnStatementNode(start, start, TypeIdentifierNode.unit(start))
		}

		val expr = ExpressionRule.defaultValue.parse(context)
		
		return ReturnStatementNode(start, expr.lastToken, expr)
	}
}