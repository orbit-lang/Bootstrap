package org.orbit.frontend.rules

import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.PairNode
import org.orbit.frontend.components.ParseError
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.extensions.unaryPlus

object PairRule : ParseRule<PairNode> {
	sealed class Errors {
		data class MissingIdentifier(override val sourcePosition: SourcePosition) : ParseError("Expected to find identifier as part of (t T) pair expression", sourcePosition)
		data class MissingType(override val sourcePosition: SourcePosition) : ParseError("Expected to find type as part of (t T) pair expression", sourcePosition)
	}

	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.peek()

		val identifierNode = context.attempt(IdentifierRule)
			?: throw context.invocation.make(PairRule.Errors.MissingIdentifier(start.position))
		
		val typeIdentifierNode = context.attempt(TypeIdentifierRule.LValue)
			?: throw context.invocation.make(PairRule.Errors.MissingType(start.position))

		return +PairNode(start, typeIdentifierNode.lastToken, identifierNode, typeIdentifierNode)
	}
}