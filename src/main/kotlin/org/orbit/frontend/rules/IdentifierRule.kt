package org.orbit.frontend.rules

import org.orbit.core.nodes.IdentifierNode
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

object IdentifierRule : ValueRule<IdentifierNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		var start = context.peek()

		if (start.type != TokenTypes.Identifier) return ParseRule.Result.Failure.Abort

		start = context.expect(TokenTypes.Identifier)

		val index = if (context.hasMore && context.peek().type == TokenTypes.Colon) {
			context.expect(TokenTypes.Colon)
			context.expect(TokenTypes.Int).text.toInt()
		} else 0

		return +IdentifierNode(start, start, start.text, index)
	}
}