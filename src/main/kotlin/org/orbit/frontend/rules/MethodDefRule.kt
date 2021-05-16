package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.extensions.unaryPlus

object MethodDefRule : ParseRule<MethodDefNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.peek()

		val signature = context.attempt(MethodSignatureRule(false), true)
			?: TODO("@MethodDefRule:18")

		val blockRule = BlockRule(PrintRule, ReturnRule, AssignmentRule, CallRule)
		val body = context.attempt(blockRule, true)
			?: TODO("@MethodDefRule:23")

		return +MethodDefNode(start, body.lastToken, signature, body)
	}
}