package org.orbit.frontend.rules

import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes

object WithinRule : ParseRule<TypeIdentifierNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		context.expect(TokenTypes.Within)
		
		return TypeIdentifierRule.RValue.execute(context)
	}
}