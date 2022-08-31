package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.phase.Parser

object ImportRule : ParseRule<TypeIdentifierNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		context.expect(TokenTypes.With)

		return TypeIdentifierRule.RValue.execute(context)
	}
}