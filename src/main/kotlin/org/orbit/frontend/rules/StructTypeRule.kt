package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.StructTypeNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object StructTypeRule : ValueRule<StructTypeNode> {
	override fun parse(context: Parser): ParseRule.Result {
		val collector = context.startCollecting()
		val delim = DelimitedRule(TokenTypes.LBrace, TokenTypes.RBrace, PairRule)
		val delimResult = context.attempt(delim)
			?: return ParseRule.Result.Failure.Rewind(collector)

		return +StructTypeNode(delimResult.firstToken, delimResult.lastToken, delimResult.nodes)
	}
}