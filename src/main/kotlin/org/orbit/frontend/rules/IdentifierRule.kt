package org.orbit.frontend.rules

import org.orbit.frontend.Parser
import org.orbit.frontend.ParseRule
import org.orbit.frontend.TokenTypes
import org.orbit.core.nodes.*

object IdentifierRule : ValueRule<IdentifierNode> {
	override fun parse(context: Parser) : IdentifierNode {
		val start = context.expect(TokenTypes.Identifier)
		
		return IdentifierNode(start, start, start.text)
	}
}