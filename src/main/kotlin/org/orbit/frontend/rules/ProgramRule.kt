package org.orbit.frontend.rules

import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*

object ProgramRule : ParseRule<ProgramNode> {
	override fun parse(context: Parser) : ProgramNode {
		val apiDef = context.attempt(ApiDefRule, true) ?: throw Exception("")
		
		return ProgramNode(listOf(apiDef))
	}
}