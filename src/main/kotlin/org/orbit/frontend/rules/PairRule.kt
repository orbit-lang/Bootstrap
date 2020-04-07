package org.orbit.frontend.rules

import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.rules.*
import org.orbit.core.nodes.*
import org.orbit.core.SourcePosition
import org.orbit.frontend.ParseError

object PairRule : ParseRule<PairNode> {
	sealed class Errors {
		data class MissingIdentifier(override val position: SourcePosition) : ParseError("Expected to find identifier as part of (t T) pair expression", position)
		data class MissingType(override val position: SourcePosition) : ParseError("Expected to find type as part of (t T) pair expression", position)
	}

	override fun parse(context: Parser) : PairNode {
		val start = context.peek()

		val identifierNode = context.attempt(IdentifierRule)
			?: throw context.invocation.make(PairRule.Errors.MissingIdentifier(start.position))
		
		val typeIdentifierNode = context.attempt(TypeIdentifierRule.LValue)
			?: throw context.invocation.make(PairRule.Errors.MissingType(start.position))

		return PairNode(start, typeIdentifierNode.lastToken, identifierNode, typeIdentifierNode)
	}
}