package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.RealLiteralNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object RealLiteralRule : ValueRule<RealLiteralNode> {
	override fun parse(context: Parser): ParseRule.Result {
		val start = context.expect(TokenTypes.Real)

		return +RealLiteralNode(start, start, start.text.toDouble())
	}
}