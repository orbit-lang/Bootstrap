package org.orbit.frontend.rules

import org.orbit.core.nodes.IExpressionNode
import org.orbit.core.nodes.RValueNode
import org.orbit.core.nodes.RealLiteralNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

class LiteralRule(private vararg val accepts: ValueRule<*> = default) : ValueRule<RValueNode> {
	private companion object {
		val default = arrayOf<ValueRule<*>>(
			TupleLiteralRule,
			InvokableReferenceRule,
			IdentifierRule,
			RealLiteralRule,
			IntLiteralRule,
			BoolLiteralRule,
			SymbolRule,
			CollectionLiteralRule
		)
	}

	override fun parse(context: Parser) : ParseRule.Result {
		context.mark()
		val expr = context.attemptAny(*accepts)
			as? IExpressionNode
			?: return ParseRule.Result.Failure.Rewind(context.end())

		return +RValueNode(expr)
	}
}