package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.Parser
import org.orbit.frontend.ParseRule

interface ValueRule<E: ExpressionNode> : ParseRule<E>

class ExpressionRule(vararg val valueRules: ValueRule<*>) : ParseRule<ExpressionNode> {
	companion object {
		// TODO - Add all rvalue rules
		val Default = ExpressionRule(
			TypeIdentifierRule
		)
	}

	override fun parse(context: Parser) : ExpressionNode {
		return context.attemptAny(*valueRules) as? ExpressionNode
			?: throw Exception("TODO")
	}
}
