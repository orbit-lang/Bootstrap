package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes

object TypeIdentifierRule : ParseRule<TypeIdentifierNode> {
	override fun parse(context: Parser) : TypeIdentifierNode {
		val token = context.expect(TokenTypes.TypeIdentifier)

		return TypeIdentifierNode(token.text)
	}
}