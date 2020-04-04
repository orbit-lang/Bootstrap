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

		var signatures = emptyList<MethodSignatureNode>()

		if (next.type == TokenTypes.LBrace) {
			val blockStartPosition = next.position
			context.consume()
			next = context.peek()

			if (next.type == TokenTypes.RBrace) {
				context.warn(Warning("Trait does not declare any method signatures, redundant braces can be deleted.", blockStartPosition))
			}

			while (next.type != TokenTypes.RBrace) {
				val signature = context.attempt(MethodSignatureRule(false))
					?: throw Exception("TODO")

				signatures += signature
				next = context.peek()
			}

			end = context.expect(TokenTypes.RBrace)
		}
		
		return TraitDefNode(start, end,
			typeIdentifierNode,
			propertyPairs, signatures)
	}
}