package org.orbit.frontend.rules

import org.orbit.core.nodes.MethodSignatureNode
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TraitDefNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

class TraitDefRule(override val isRequired: Boolean = false) : EntityParseRule<TraitDefNode> {
	companion object {
		val required = TraitDefRule(true)
	}

	override fun parse(context: Parser) : ParseRule.Result {
		val start = when (isRequired) {
			true -> context.expect(TokenTypes.Required, true)
			else -> context.expect(TokenTypes.Trait)
		}
		
		val typeIdentifierNode = context.attempt(TypeIdentifierRule.LValue)
			?: TODO("@TraitDefRule:18")

		var next = context.peek()
		val propertyPairs = mutableListOf<PairNode>()

		var end = typeIdentifierNode.lastToken

		if (next.type == TokenTypes.LParen) {

			if (context.peek(1).type == TokenTypes.RParen) {
				context.consume()
				context.consume()

				return +TraitDefNode(start, end, isRequired, typeIdentifierNode)
			}

			// NOTE - Same ambiguity as TypeDef
			val lookaheadParser = Parser(context.invocation, MethodSignatureRule(false))

			try {
				lookaheadParser.execute(Parser.InputType(context.tokens))

				return +TraitDefNode(start, end, isRequired, typeIdentifierNode)
			} catch (_: Exception) {
				// fallthrough
			}

			context.consume()

			while (true) {
				val propertyPair = context.attempt(PairRule)
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

		var traitConformances = mutableListOf<TypeIdentifierNode>()

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
			return +TraitDefNode(start, bodyNode.lastToken, isRequired, typeIdentifierNode, propertyPairs, traitConformances, bodyNode.body as List<MethodSignatureNode>)
		}
		
		return +TraitDefNode(start, end, isRequired,
			typeIdentifierNode,
			propertyPairs, traitConformances)
	}
}