package org.orbit.frontend.rules

import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.TokenTypes

object ProgramRule : ParseRule<ProgramNode> {
	override fun parse(context: Parser) : ProgramNode {
		val start = context.peek()
		var next = context.peek()
		var apiDefs = emptyList<ApiDefNode>()
		
		while (next.type == TokenTypes.Api) {
			val apiDef = context.attempt(ApiDefRule, true)
				?: throw Exception("Expected api at top level")
			
			apiDefs += apiDef
			if (!context.hasMore) break
			
			next = context.peek()
		}
		
		return ProgramNode(start, next, apiDefs)
	}
}