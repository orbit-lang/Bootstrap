package org.orbit.frontend.rules

import org.orbit.frontend.Parser
import org.orbit.frontend.ParseRule
import org.orbit.frontend.TokenTypes
import org.orbit.core.nodes.TypeIdentifierNode

object WithRule : ParseRule<TypeIdentifierNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		context.expect(TokenTypes.With)

		return TypeIdentifierRule.RValue.execute(context)
	}
}