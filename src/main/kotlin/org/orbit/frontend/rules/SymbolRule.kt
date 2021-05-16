package org.orbit.frontend.rules

import org.orbit.core.nodes.SymbolLiteralNode
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

object SymbolRule : ValueRule<SymbolLiteralNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expect(TokenTypes.Colon)
		val value = context.expectAny(TokenTypes.Identifier, TokenTypes.TypeIdentifier, consumes = true)

		// `Symbol(length Int, str String)`
		return +SymbolLiteralNode(start, start,
			value.text.length, value.text)
	}
}