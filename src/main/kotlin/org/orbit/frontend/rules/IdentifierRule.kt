package org.orbit.frontend.rules

import org.orbit.frontend.Parser
import org.orbit.frontend.ParseRule
import org.orbit.frontend.TokenTypes
import org.orbit.core.nodes.*

object IdentifierRule : ParseRule<IdentifierNode> {
	override fun parse(context: Parser) : IdentifierNode {
		val token = context.expect(TokenTypes.Identifier)
		
		return IdentifierNode(token.text)
	}
}