package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.Parser
import org.orbit.frontend.ParseRule
import org.orbit.frontend.TokenTypes

object SymbolRule : ValueRule<SymbolLiteralNode> {
	override fun parse(context: Parser) : SymbolLiteralNode {
		val start = context.expect(TokenTypes.Symbol)
		val value = start.text.substring(1)

		// `Symbol(length Int, str String)`
		return SymbolLiteralNode(start, start,
			value.length, value)
	}
}