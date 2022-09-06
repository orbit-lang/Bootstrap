package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.phase.Parser

object ImportRule : ParseRule<TypeIdentifierNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expect(TokenTypes.With)

		// TODO - Parse File globs, e.g. `Orb::{Core,System}`
//		if (!context.hasMore) {
//			return ParseRule.Result.Failure.Throw("Expected something to import after `with...`", start)
//		}
//
//		val next = context.peek()
//
//		if (next.type == )

		return TypeIdentifierRule.RValue.execute(context)
	}
}