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

object TraitDefRule : ParseRule<TraitDefNode> {
	override fun parse(context: Parser) : TraitDefNode {
		val start = context.expect(TokenTypes.Trait)
		
		val typeIdentifierNode = context.attempt(TypeIdentifierRule)
			?: throw Exception("TODO")

		var next = context.peek()
		var propertyPairs = emptyList<PairNode>()

		var end = typeIdentifierNode.lastToken

		if (next.type == TokenTypes.LParen) {
			// NOTE - Same ambiguity as TypeDef
			val lookaheadParser = Parser(MethodSignatureRule(false))

			try {
				lookaheadParser.execute(context.tokens)

				return TraitDefNode(start, end, typeIdentifierNode)
			} catch (_: Exception) {
				// fallthrough
			}

			context.consume()

			while (true) {
				val propertyPair = context.attempt(PairRule)
					?: throw Exception("TODO")

				propertyPairs += propertyPair

				next = context.peek()

				if (next.type == TokenTypes.Comma) {
					context.consume()
				} else {
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
			val bodyNode = context.attempt(BlockRule(TypeDefRule, TraitDefRule, MethodSignatureRule(false)), true)
				?: throw Exception("TODO")
			
			return TraitDefNode(start, bodyNode.lastToken, typeIdentifierNode, propertyPairs, traitConformances, bodyNode)
		}
		
		return TraitDefNode(start, end,
			typeIdentifierNode,
			propertyPairs, traitConformances)
	}
}