package org.orbit.frontend.rules

import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.ParseError
import org.orbit.frontend.rules.PairRule
import org.orbit.core.SourcePosition
import org.orbit.core.Warning

class BlockRule(private vararg val bodyRules: ParseRule<*>) : ParseRule<BlockNode> {
	override fun parse(context: Parser) : BlockNode {
		val start = context.expect(TokenTypes.LBrace)
		var next = context.peek()
		var body = mutableListOf<Node>()
		
		while (next.type != TokenTypes.RBrace) {
			for (rule in bodyRules) {
				//context.startRecording()

				val node = context.attempt(rule)

				if (node != null) {
					body.add(node)
					break
				}
			}

			next = context.peek()
		}

		val end = context.expect(TokenTypes.RBrace)

		return BlockNode(start, end, body)
	}
}