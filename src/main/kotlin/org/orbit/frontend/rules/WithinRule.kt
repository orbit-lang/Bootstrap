package org.orbit.core.nodes

import org.orbit.frontend.rules.TypeIdentifierRule
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser

object WithinRule : ParseRule<TypeIdentifierNode> {
	override fun parse(context: Parser) : TypeIdentifierNode {
		context.expect(TokenTypes.Within)
		
		return TypeIdentifierRule.RValue.execute(context)
	}
}