package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.Parser
import org.orbit.frontend.ParseRule
import org.orbit.frontend.TokenTypes
import java.lang.Exception

class LiteralRule(private vararg val accepts: ValueRule<*> = LiteralRule.Default) : ParseRule<RValueNode> {
	private companion object {
		val Default = arrayOf<ValueRule<*>>(
			TypeIdentifierRule.RValue,
			IdentifierRule,
			IntLiteralRule,
			SymbolRule
		)
	}

	override fun parse(context: Parser) : RValueNode {
		val start = context.peek()
		val expr = context.attemptAny(*accepts, throwOnNull =  true)
			as? ExpressionNode ?: throw Exception("TODO")

		val next = context.peek()

		if (next.type == TokenTypes.LAngle) {
			val typeParametersNode = TypeParametersRule(true).parse(context)

			return RValueNode(start, typeParametersNode.lastToken, expr, typeParametersNode)
		}

		return RValueNode(expr)
	}
}