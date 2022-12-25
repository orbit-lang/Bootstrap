package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.IntLiteralNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object IntLiteralRule : ValueRule<IntLiteralNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expect(TokenTypes.Int)
		
		return +IntLiteralNode(start, start, start.text.toInt())
	}
}

