package org.orbit.frontend.rules

import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.nodes.RValueNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

class LiteralRule(private vararg val accepts: ValueRule<*> = Default) : ValueRule<RValueNode> {
	private companion object {
		val Default = arrayOf<ValueRule<*>>(
			InvokableReferenceRule,
			TypeIdentifierRule.Naked,
			IdentifierRule,
			IntLiteralRule,
			SymbolRule,
			CollectionLiteralRule
		)
	}

	override fun parse(context: Parser) : ParseRule.Result {
		context.mark()
		val expr = context.attemptAny(*accepts)
			as? ExpressionNode
			?: return ParseRule.Result.Failure.Rewind(context.end())

		if (!context.hasMore) return +RValueNode(expr)

		return +RValueNode(expr)
	}
}