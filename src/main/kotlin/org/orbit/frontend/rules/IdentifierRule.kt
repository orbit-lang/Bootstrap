package org.orbit.frontend.rules

import org.orbit.core.nodes.IdentifierNode
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

object IdentifierRule : ValueRule<IdentifierNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expectOrNull(TokenTypes.Identifier)
			?: return ParseRule.Result.Failure.Rewind()

		return +IdentifierNode(start, start, start.text)
	}
}