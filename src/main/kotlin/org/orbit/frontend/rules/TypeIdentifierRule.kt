package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes

object TypeIdentifierRule : ParseRule<TypeIdentifierNode> {
	override fun parse(context: Parser) : TypeIdentifierNode {
		val start = context.expect(TokenTypes.TypeIdentifier)

		val next = context.peek()

		if (next.type == TokenTypes.LAngle) {
			val typeParametersNode = context.attempt(TypeParametersRule, true)
				?: throw Exception("TODO")

			return TypeIdentifierNode(start, typeParametersNode.lastToken, start.text, typeParametersNode)
		}
		
		return TypeIdentifierNode(start, start, start.text)
	}
}