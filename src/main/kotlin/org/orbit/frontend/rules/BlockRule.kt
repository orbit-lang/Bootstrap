package org.orbit.frontend.rules

import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.ParseError
import org.orbit.frontend.rules.PairRule
import org.orbit.core.SourcePosition
import org.orbit.core.Token
import org.orbit.core.Warning

class BlockRule(vararg val bodyRules: ParseRule<*>) : ParseRule<BlockNode> {
	sealed class Errors {
		data class UnexpectedBlockStatement(private val token: Token)
			: ParseError("Unexpected token inside block: ${token.type}", token.position)
	}
	
	override fun parse(context: Parser) : BlockNode {
		val start = context.expect(TokenTypes.LBrace)
		var next = context.peek()
		var body = mutableListOf<Node>()
		
		while (next.type != TokenTypes.RBrace) {
			body.add(context.attemptAny(*bodyRules, throwOnNull = true)
				?: throw context.invocation.make(BlockRule.Errors.UnexpectedBlockStatement(next)))

			next = context.peek()
		}

		val end = context.expect(TokenTypes.RBrace)
		
		return BlockNode(start, end, body)
	}
}