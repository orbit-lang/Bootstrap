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
	
	sealed class Warnings {
		data class EmptyApi(val name: String, override val position: SourcePosition)
			: Warning("Api '$name' is empty and may be erased by a later compilation phase", position)
	}

	override fun parse(context: Parser) : ApiDefNode {
		val start = context.expect(TokenTypes.Api)
		val typeIdentifierNode = context.attempt(TypeIdentifierRule)
			?: throw ApiDefRule.Errors.MissingName(start.position)

		// TODO - Parser 'with' & 'within' statements

		val withinNode = context.attempt(WithinRule)
		var with = context.attempt(WithRule)
		var withNodes = mutableListOf<TypeIdentifierNode>()
		
		while (with != null) {
			withNodes.add(with)
			with = context.attempt(WithRule)
		}
		
		context.expect(TokenTypes.LBrace)
		
		var typeDefNodes = mutableListOf<TypeDefNode>()
		var next: Token = context.peek()

		while (next.type != TokenTypes.RBrace) {
			// TODO - Parse other api level expressions
			when (next.type) {
				TokenTypes.Type -> {
					val typeDefNode = context.attempt(TypeDefRule)
						?: throw Exception("???: ${next.type.identifier}")

					typeDefNodes.add(typeDefNode)
				}

				else -> throw Exception("Unexpected token: $next")
			}

			next = context.peek()
		}

		context.expect(TokenTypes.RBrace)

		if (typeDefNodes.isEmpty()) {
			context.warn(ApiDefRule.Warnings.EmptyApi(
				typeIdentifierNode.typeIdentifier, start.position))
		}
		
		val node = ApiDefNode(typeIdentifierNode, typeDefNodes)

		if (withinNode != null) {
			node.annotate(withinNode, KeyedNodeAnnotationTag("api.within"))
		}

		if (withNodes.isNotEmpty()) {
			node.annotate(withNodes, KeyedNodeAnnotationTag("api.with"))
		}

		return node
	}
}