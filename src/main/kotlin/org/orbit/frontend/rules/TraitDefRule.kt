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

		if (next.type == TokenTypes.LParen) {
			// NOTE - Same ambiguity as TypeDef
			context.startRecording()
			if (context.attempt(MethodSignatureRule(false)) != null) {
				context.autoRewind()

				return TraitDefNode(typeIdentifierNode)
			}

			context.autoRewind()
			context.consume()

			next = context.peek()

			while (true) {
				val propertyPair = context.attempt(PairRule)
					?: throw Exception("TODO")

				propertyPairs += propertyPair

				next = context.peek()

				if (next.type == TokenTypes.Comma) {
					context.consume()
				} else {
					context.expect(TokenTypes.RParen)
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

			context.expect(TokenTypes.RBrace)
		}

		return TraitDefNode(typeIdentifierNode, propertyPairs, signatures)
	}
}