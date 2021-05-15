package org.orbit.frontend.rules

import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.nodes.RValueNode
import org.orbit.core.nodes.TypeParametersNode
import org.orbit.frontend.*

class LiteralRule(private vararg val accepts: ValueRule<*> = Default) : ValueRule<RValueNode> {
	private companion object {
		val Default = arrayOf<ValueRule<*>>(
			MethodReferenceRule,
			TypeIdentifierRule.RValue,
			IdentifierRule,
			IntLiteralRule,
			SymbolRule
		)
	}

	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.peek()
		val expr = context.attemptAny(*accepts, throwOnNull = true)
			as? ExpressionNode
			?: TODO("@LiteralRule:22")

		if (!context.hasMore) return +RValueNode(expr)

		val next = context.peek()

		if (next.type == TokenTypes.LAngle) {
			val typeParametersNode = TypeParametersRule(true)
				.execute(context)
				.unwrap<ParseRule.Result.Success<TypeParametersNode>>()
				?.node
				?: return ParseRule.Result.Failure.Abort

			return parseTrailing(context, RValueNode(start, typeParametersNode.lastToken, expr, typeParametersNode))
		}

		return parseTrailing(context, RValueNode(expr))
	}
}