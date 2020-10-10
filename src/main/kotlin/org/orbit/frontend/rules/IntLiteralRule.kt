package org.orbit.frontend.rules

import org.json.JSONObject
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
import org.orbit.serial.Serial

object IntLiteralRule : ValueRule<IntLiteralNode> {
	override fun parse(context: Parser) : IntLiteralNode {
		val start = context.expect(TokenTypes.Int)
		
		return IntLiteralNode(start, start, start.text.toBigInteger())
	}
}