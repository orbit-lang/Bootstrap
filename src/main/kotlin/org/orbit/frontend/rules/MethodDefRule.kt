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

object MethodDefRule : ParseRule<MethodDefNode> {
	override fun parse(context: Parser) : MethodDefNode {
		val start = context.peek()

		val signature = context.attempt(MethodSignatureRule(false), true)
			?: throw Exception("TODO")

		// TODO - Parse actual block statements
		val body = context.attempt(BlockRule(TypeIdentifierRule), true)
			?: throw Exception("TODO")

		return MethodDefNode(start, body.lastToken, signature, body)
	}
}