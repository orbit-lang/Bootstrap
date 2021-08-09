package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.core.components.SourcePosition
import org.orbit.frontend.components.ParseError
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.extensions.unaryPlus

interface EntityParseRule<E: EntityDefNode> : PrefixPhaseAnnotatedParseRule<E> {
	companion object {
		val moduleTopLevelRules = listOf<ParseRule<*>>(TypeConstructorRule, TypeProjectionRule, TraitConstructorRule, TypeAliasRule, TypeDefRule(), TraitDefRule())
		val apiTopLevelRules = listOf<ParseRule<*>>(TypeConstructorRule, TraitConstructorRule, TypeAliasRule, TypeDefRule.required, TraitDefRule.required, TypeDefRule(), TraitDefRule())
	}

	val isRequired: Boolean
}

class TypeDefRule(override val isRequired: Boolean = false) : EntityParseRule<TypeDefNode> {
	companion object {
		val required = TypeDefRule(true)
	}

	sealed class Errors {
		data class MissingName(override val sourcePosition: SourcePosition)
			: ParseError("Type definition requires a name", sourcePosition)
			
		data class MissingPair(override val sourcePosition: SourcePosition)
			: ParseError("Expected property declarations following type definition", sourcePosition)
	}

	override fun parse(context: Parser) : ParseRule.Result {
		val start = when (isRequired) {
			true -> context.expect(TokenTypes.Required, true)
			else -> context.expect(TokenTypes.Type)
		}

		if (isRequired) context.expect(TokenTypes.Type)

		val typeIdentifierNode = context.attempt(TypeIdentifierRule.LValue, true)
			?: throw context.invocation.make(Errors.MissingName(start.position))

		var next = context.peek()
		var propertyPairs = emptyList<PairNode>()

		// The last token in this TypeDef could be in a number of different places.
		// By default, it is the typeIdentifierNode's last token (assumes `type A`)
		var end = typeIdentifierNode.lastToken

		if (next.type == TokenTypes.LParen) {
			if (context.peek(1).type == TokenTypes.RParen) {
				// No parameters defined, eat the parens and return
				context.consume()
				context.consume()

				return +TypeDefNode(start, end, isRequired, typeIdentifierNode)
			}

			// NOTE - We have an ambiguous grammar here.
			/*
				EXAMPLE:
					type T
					(T) foo () (T)

				The method signature's receiver parses as a continuation of the TypeDef, e.g. type T(T).

				We can actually disambiguate this situation, but maybe TypeDefs without parameters should
				be forced to add empty parens, e.g. type T()
			*/

			// We use a separate parser here to avoid popping from our own token stack,
			// which would otherwise be quite difficult to rewind if the following case parses

			val lookaheadParser = Parser(context.invocation, MethodSignatureRule(false))

			try {
				lookaheadParser.execute(Parser.InputType(context.tokens))

				// This is the ambiguous case described above.
				// We can jump out here, safe in the knowledge that
				// doing a lookahead parse did not affect the main token stack
				return +TypeDefNode(start, end, isRequired, typeIdentifierNode)
			} catch (_: Exception) {
				// This is not a real parse error; it just means this isn't the ambiguous case (see above).
				// fallthrough
			}

			// NOTE - Don't forget to move beyond the paren as we only peeked at it before now
			context.consume()

			while (true) {
				val propertyPair = context.attempt(PairRule)
					?: throw context.invocation.make(TypeDefRule.Errors.MissingPair(start.position))

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

		var traitConformances = mutableListOf<TypeExpressionNode>()

		if (next.type == TokenTypes.Colon) {
			context.consume()

			next = context.peek()

			while (next.type == TokenTypes.TypeIdentifier) {
				val traitConformance = context.attempt(TypeExpressionRule)
					?: throw context.invocation.make(Parser.Errors.UnexpectedToken(next))

				traitConformances.add(traitConformance)

				// Another potential lastToken (assumes `type A : B`)
				end = traitConformance.lastToken

				next = context.peek()

				if (next.type == TokenTypes.Comma) {
					context.consume()

					next = context.peek()

					if (next.type != TokenTypes.TypeIdentifier) {
						// Dangling comma
						// TODO - Better error message
						throw context.invocation.make(Parser.Errors.UnexpectedToken(next))
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
			val bodyNode = context.attempt(BlockRule(TraitDefRule(isRequired), TypeDefRule(isRequired), MethodSignatureRule(false)), true)
				?: TODO("@TypeDefRule:128")

			return +TypeDefNode(start, bodyNode.lastToken, isRequired, typeIdentifierNode, propertyPairs, traitConformances, bodyNode)
		}
		
		return +TypeDefNode(start, end, isRequired, typeIdentifierNode, propertyPairs, traitConformances)
	}
}