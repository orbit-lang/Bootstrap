package org.orbit.frontend.rules

import org.orbit.backend.typesystem.components.IType
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

		val next = context.peek()

		val size = when (next.text) {
			"/" -> {
				// This Collection Type has an explicit size constraint
				// TODO - Generalising this concept for Dependent Types
				context.consume()

				val end = context.attempt(IntLiteralRule)
					?: return ParseRule.Result.Failure.Throw("Expected Int literal after `[${elementType.value}/...`", collector)

				IType.Array.Size.Fixed(end.value.second)
			}

			else -> IType.Array.Size.Any
		}

		val end = context.expect(TokenTypes.RBracket)

		return +CollectionTypeNode(start, end, elementType, size)
	}
}