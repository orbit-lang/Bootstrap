package org.orbit.frontend.rules

import org.orbit.core.nodes.IntLiteralNode
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.unaryPlus

object IntLiteralRule : ValueRule<IntLiteralNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expect(TokenTypes.Int)
		
		return +IntLiteralNode(start, start, start.text.toBigInteger())
	}
}