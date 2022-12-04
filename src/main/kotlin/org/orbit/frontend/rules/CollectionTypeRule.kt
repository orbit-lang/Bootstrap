package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.CollectionTypeNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object CollectionTypeRule : ValueRule<CollectionTypeNode> {
	override fun parse(context: Parser): ParseRule.Result {
		val collector = context.startCollecting()
		val start = context.expect(TokenTypes.LBracket)
		val elementType = context.attempt(TypeExpressionRule)
			?: return ParseRule.Result.Failure.Rewind(collector.getCollectedTokens())

		val end = context.expect(TokenTypes.RBracket)

		return +CollectionTypeNode(start, end, elementType)
	}
}