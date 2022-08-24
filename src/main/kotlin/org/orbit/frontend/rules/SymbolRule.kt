package org.orbit.frontend.rules

import org.orbit.core.nodes.SymbolLiteralNode
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

object SymbolRule : ValueRule<SymbolLiteralNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expect(TokenTypes.Symbol)
		val symbol = start.text.drop(1)

		return +SymbolLiteralNode(start, start,
			symbol.length, symbol)
	}
}