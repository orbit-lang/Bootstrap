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

object TypeDefRule : ParseRule<TypeDefNode> {
	sealed class Errors {
		data class MissingName(override val position: SourcePosition)
			: ParseError("Type definition requires a name", position)
			
		data class MissingPair(override val position: SourcePosition)
			: ParseError("Expected property declarations following type definition", position)
	}

	override fun parse(context: Parser) : TypeDefNode {
		val start = context.expect(TokenTypes.Type)
		val typeIdentifierNode = context.attempt(TypeIdentifierRule)
			?: throw TypeDefRule.Errors.MissingName(start.position)

		var next = context.peek()
		var propertyPairs = emptyList<PairNode>()

		if (next.type == TokenTypes.LParen) {
			// NOTE - We have an ambiguous grammar here.
			/*
				EXAMPLE:
					type T
					(T) foo () (T)

				The method signature's receiver parses as a continuation of the TypeDef, e.g. type T(T).

				We can actually disambiguate this situation, but maybe TypeDefs without parameters should
				be forced to add empty parens, e.g. type T()
			*/

			context.startRecording()
			if (context.attempt(MethodSignatureRule(false)) != null) {
				// This is a positive for the above ambiguity
				context.autoRewind()

				// Next expression is definitely a method signature.
				// We know this TypeDef is complete, ambiguity disambiguated
				return TypeDefNode(typeIdentifierNode)
			}

			context.autoRewind()

			// NOTE - Don't forget to move beyond the paren as we only peeked at it before now
			context.consume()
			next = context.peek()

			while (true) {
				val propertyPair = context.attempt(PairRule)
					?: throw TypeDefRule.Errors.MissingPair(start.position)

				propertyPairs += propertyPair
				
				next = context.peek()

				if (next.type == TokenTypes.Comma) {
					// ',' here tells us to keep parsing pairs (multiple properties)
					context.consume()
				} else {
					// ')' here tells us that the list of properties is finished
					context.expect(TokenTypes.RParen)
					break
				}
			}
		}

		// TODO - Parse trait conformance, e.g. type B : A etc
		
		return TypeDefNode(typeIdentifierNode, propertyPairs)
	}
}