package org.orbit.frontend.rules

import org.orbit.core.nodes.IntLiteralNode
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

object IntLiteralRule : ValueRule<IntLiteralNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expect(TokenTypes.Int)
		
		return +IntLiteralNode(start, start, start.text.toBigInteger())
	}
}