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
		val typeIdentifierNode = context.attempt(TypeIdentifierRule, true)
			?: throw TypeDefRule.Errors.MissingName(start.position)

		var next = context.peek()
		var propertyPairs = emptyList<PairNode>()

		// The last token in this TypeDef could be in a number of different places.
		// By default, it is the typeIdentifierNode's last token (assumes `type A`)
		var end = typeIdentifierNode.lastToken

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

			// We use a separate parser here to keep our avoid popping from our own token stack,
			// which would otherwise be quite difficult to rewind if the following case parses

			val lookaheadParser = Parser(MethodSignatureRule(false))

			try {
				lookaheadParser.execute(context.tokens)

				// This is the ambiguous case described above.
				// We can jump out here, safe in the knowledge that
				// doing a lookahead parse did not affect the main token stack
				return TypeDefNode(start, end, typeIdentifierNode)
			} catch (_: Exception) {
				// This is not a real parse error; it just means this isn't the ambiguous case (see above).
				// fallthrough
			}

			// NOTE - Don't forget to move beyond the paren as we only peeked at it before now
			context.consume()

			while (true) {
				val propertyPair = context.attempt(PairRule)
					?: throw TypeDefRule.Errors.MissingPair(start.position)

				propertyPairs += propertyPair

				next = context.peek()

				if (next.type == TokenTypes.Comma) {
					// ',' here tells us to keep parsing pairs (multiple properties)
					context.consume()
				} else {
					// ')' here tells us that the list of properties is finished.
					// This is also another potential lastToken (assumes `type A(x X, ...)`)
					end = context.expect(TokenTypes.RParen)
					break
				}
			}
		}

		next = context.peek()

		var traitConformances = mutableListOf<TypeIdentifierNode>()

		if (next.type == TokenTypes.Colon) {
			context.consume()

			next = context.peek()

			while (next.type == TokenTypes.TypeIdentifier) {
				val traitConformance = context.attempt(TypeIdentifierRule)
					?: throw Parser.Errors.UnexpectedToken(next)

				traitConformances.add(traitConformance)

				// Another potential lastToken (assumes `type A : B`)
				end = traitConformance.lastToken

				next = context.peek()

				if (next.type == TokenTypes.Comma) {
					context.consume()

					next = context.peek()

					if (next.type != TokenTypes.TypeIdentifier) {
						// Dangling comma
						throw Parser.Errors.UnexpectedToken(next)
					}
				}
			}
		}

		if (next.type == TokenTypes.LBrace) {
			// NOTE - BlockRule consumes the surrounding braces
			/*
				Type body block can contain:
					- Nested type def (making this an enum type)
					- Method implementations (marking them as part of this type's public api)
			*/

			// TODO - Swap MethodSignatureRule out for MethodDefRule
			val bodyNode = context.attempt(BlockRule(TraitDefRule, TypeDefRule, MethodSignatureRule(false)), true)
				?: throw Exception("TODO")

			return TypeDefNode(start, bodyNode.lastToken, typeIdentifierNode, propertyPairs, traitConformances, bodyNode)
		}
		
		return TypeDefNode(start, end, typeIdentifierNode, propertyPairs, traitConformances)
	}
}