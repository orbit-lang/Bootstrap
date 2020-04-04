package org.orbit.frontend.rules

import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.core.nodes.*
import org.orbit.core.Token
import org.orbit.frontend.rules.*
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.ParseError
import org.orbit.core.SourcePosition
import org.orbit.core.Warning

object ApiDefRule : ParseRule<ApiDefNode> {
	sealed class Errors {
		data class MissingName(override val position: SourcePosition)
			: ParseError("Api definition requires a name", position)
	}

	override fun parse(context: Parser) : ApiDefNode {
		val start = context.expect(TokenTypes.Api)
		val typeIdentifierNode = context.attempt(TypeIdentifierRule)
			?: throw ApiDefRule.Errors.MissingName(start.position)

		val withinNode = context.attempt(WithinRule)
		var with = context.attempt(WithRule)
		var withNodes = mutableListOf<TypeIdentifierNode>()
		
		while (with != null) {
			withNodes.add(with)
			with = context.attempt(WithRule)
		}
		
		context.expect(TokenTypes.LBrace)
		
		var typeDefNodes = mutableListOf<TypeDefNode>()
		var traitDefNodes = mutableListOf<TraitDefNode>()
		var methodDefNodes = mutableListOf<MethodDefNode>()
		var next: Token = context.peek()

		while (next.type != TokenTypes.RBrace) {
			
			when (next.type) {
				TokenTypes.Type -> {
					val typeDefNode = context.attempt(TypeDefRule, true)
						?: throw Exception("Expected type decl following 'type' at api-level")

					typeDefNodes.add(typeDefNode)
				}

				TokenTypes.Trait -> {
					val traitDefNode = context.attempt(TraitDefRule, true)
						?: throw Exception("Expected trait decl following 'trait' at api-level")

					traitDefNodes.add(traitDefNode)
				}

				// Method defs
				TokenTypes.LParen -> {
					val methodDefNode = context.attempt(MethodDefRule, true)
						?: throw Exception("Expected method signature following '(' at api-level")

					methodDefNodes.add(methodDefNode)
				}

				else -> throw Exception("Unexpected token: $next")
			}

			next = context.peek()
		}

		val end = context.expect(TokenTypes.RBrace)
		
		return ApiDefNode(start, end,
			typeIdentifierNode, typeDefNodes, traitDefNodes, methodDefNodes, withinNode, withNodes)
	}
}