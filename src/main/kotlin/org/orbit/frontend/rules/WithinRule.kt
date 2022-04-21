package org.orbit.frontend.rules

import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes

object WithinRule : ParseRule<TypeIdentifierNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		context.expect(TokenTypes.Within)
		
		return TypeIdentifierRule.RValue.execute(context)
	}
}