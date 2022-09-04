package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.core.components.SourcePosition
import org.orbit.core.components.TokenType
import org.orbit.frontend.components.ParseError
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.extensions.unaryPlus

interface EntityDefParseRule<E: EntityDefNode> : ParseRule<E> {
	companion object {
		val moduleTopLevelRules = listOf<ParseRule<*>>(ProjectionRule, TypeAliasRule, TypeDefRule, TraitDefRule, FamilyRule)
		val apiTopLevelRules = listOf<ParseRule<*>>(TypeAliasRule, TraitDefRule, TypeDefRule, TraitDefRule)
	}
}

object TypeDefRule : EntityDefParseRule<TypeDefNode> {
	sealed class Errors {
		data class MissingName(override val sourcePosition: SourcePosition)
			: ParseError("Type definition requires a name", sourcePosition)
			
		data class MissingPair(override val sourcePosition: SourcePosition)
			: ParseError("Expected property declarations following type definition", sourcePosition)
	}

	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expect(TokenTypes.Type)

		val typeIdentifierNode = context.attempt(TypeIdentifierRule.LValue, true)
			?: throw context.invocation.make(Errors.MissingName(start.position))

		var next = context.peek()
		val propertyPairs = mutableListOf<ParameterNode>()

		// The last token in this TypeDef could be in a number of different places.
		// By default, it is the typeIdentifierNode's last token (assumes `type A`)
		var end = typeIdentifierNode.lastToken

		if (next.type == TokenTypes.LParen) {
			if (context.peek(1).type == TokenTypes.RParen) {
				// No parameters defined, eat the parens and return
				context.consume()
				context.consume()

				return +TypeDefNode(start, end, typeIdentifierNode)
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
				return +TypeDefNode(start, end, typeIdentifierNode)
			} catch (_: Exception) {
				// This is not a real parse error; it just means this isn't the ambiguous case (see above).
				// fallthrough
			}

			// NOTE - Don't forget to move beyond the paren as we only peeked at it before now
			context.consume()

			while (true) {
				val propertyPair = context.attempt(ParameterRule)
					?: throw context.invocation.make(Errors.MissingPair(start.position))

				propertyPairs.add(propertyPair)

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

		if (!context.hasMore) {
			return +TypeDefNode(start, end, typeIdentifierNode, propertyPairs, emptyList())
		}

		next = context.peek()

		val traitConformances = mutableListOf<TypeExpressionNode>()

		if (next.type == TokenTypes.Colon) {
			context.consume()

			next = context.peek()

			while (next.type == TokenTypes.TypeIdentifier || next.type.family == TokenType.Family.CompileTime) {
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
			val bodyNode = context.attempt(AlgebraicConstructorRule.toBlockRule())
				?: return ParseRule.Result.Failure.Throw("Only Algebraic Constructors are allowed in the body of a Type declaration", next)

			return +TypeDefNode(start, bodyNode.lastToken, typeIdentifierNode, propertyPairs, traitConformances, bodyNode.body as List<ITypeDefBodyNode>)
		}
		
		return +TypeDefNode(start, end, typeIdentifierNode, propertyPairs, traitConformances)
	}
}