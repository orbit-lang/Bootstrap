package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

object ReturnRule : ParseRule<ReturnStatementNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expectOrNull(TokenTypes.Return)
			?: return ParseRule.Result.Failure.Rewind()

		val next = context.peek()

		if (next.type == TokenTypes.RBrace) {
			// This is an empty return, `return`,
			// syntactic sugar for `return Unit`
			return +ReturnStatementNode(start, start, TypeIdentifierNode.unit(start))
		}

		val expr = context.attempt(ExpressionRule.defaultValue)
			?: throw context.invocation.make<Parser>("Expected return expression", context.peek().position)
		
		return +ReturnStatementNode(start, expr.lastToken, expr)
	}
}