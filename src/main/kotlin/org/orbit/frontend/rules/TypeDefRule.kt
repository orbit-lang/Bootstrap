package org.orbit.frontend.rules

import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.ParseError
import org.orbit.core.SourcePosition
import org.orbit.core.Warning

object TypeDefRule : ParseRule<TypeDefNode> {
	sealed class Errors {
		data class MissingName(override val position: SourcePosition)
			: ParseError("Type definition requires a name", position)
	}

	override fun parse(context: Parser) : TypeDefNode {
		val start = context.expect(TokenTypes.Type)
		val typeIdentifierNode = context.attempt(TypeIdentifierRule)
			?: throw TypeDefRule.Errors.MissingName(start.position)

		// TODO - Parse trait conformance, e.g. type B : A etc
		// TODO - Parse type properties, e.g. type A(x Int) etc
		return TypeDefNode(typeIdentifierNode)
	}
}