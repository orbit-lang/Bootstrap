package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.rules.PairRule
import org.orbit.core.SourcePosition
import org.orbit.core.Warning
import org.orbit.frontend.*

object MethodDefRule : ParseRule<MethodDefNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.peek()

		val signature = context.attempt(MethodSignatureRule(false), true)
			?: TODO("@MethodDefRule:18")

		// TODO - Parse actual block statements
		val blockRule = BlockRule(ReturnRule, AssignmentRule, InstanceMethodCallRule)
		val body = context.attempt(blockRule, true)
			?: TODO("@MethodDefRule:23")

		return +MethodDefNode(start, body.lastToken, signature, body)
	}
}