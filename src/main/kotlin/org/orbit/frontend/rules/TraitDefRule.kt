package org.orbit.frontend.rules

import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus

object TraitDefRule : EntityDefParseRule<TraitDefNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expect(TokenTypes.Trait)
		
		val typeIdentifierNode = context.attempt(TypeIdentifierRule.LValue)
			?: TODO("@TraitDefRule:18")

		var next = context.peek()
		val propertyPairs = mutableListOf<ParameterNode>()

		var end = typeIdentifierNode.lastToken

		if (next.type == TokenTypes.LParen) {

			if (context.peek(1).type == TokenTypes.RParen) {
				context.consume()
				context.consume()

				return +TraitDefNode(start, end, typeIdentifierNode)
			}

			// NOTE - Same ambiguity as TypeDef
			val lookaheadParser = Parser(context.invocation, MethodSignatureRule(false))

			try {
				lookaheadParser.execute(Parser.InputType(context.tokens))

				return +TraitDefNode(start, end, typeIdentifierNode)
			} catch (_: Exception) {
				// fallthrough
			}

			context.consume()

			while (true) {
				val propertyPair = context.attempt(ParameterRule())
					?: TODO("@TraitDefRule:41")

				propertyPairs.add(propertyPair)

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

		val traitConformances = mutableListOf<TypeIdentifierNode>()

		if (next.type == TokenTypes.Colon) {
			context.consume()

			next = context.peek()

			while (next.type == TokenTypes.TypeIdentifier) {
				val traitConformance = context.attempt(TypeIdentifierRule.LValue)
					?: throw context.invocation.make(Parser.Errors.UnexpectedToken(next))

				traitConformances.add(traitConformance)

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
			val bodyNode = context.attempt(BlockRule(MethodSignatureRule(false)), true)
				?: TODO("TraitDefRule:91")

			@Suppress("UNCHECKED_CAST")
			return +TraitDefNode(start, bodyNode.lastToken, typeIdentifierNode, propertyPairs, traitConformances, bodyNode.body as List<MethodSignatureNode>)
		}
		
		return +TraitDefNode(start, end, typeIdentifierNode, propertyPairs, traitConformances)
	}
}