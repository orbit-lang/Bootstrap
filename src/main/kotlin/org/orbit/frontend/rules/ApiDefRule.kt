package org.orbit.frontend.rules

import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.core.nodes.*
import org.orbit.core.Token
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.ParseError
import org.orbit.core.SourcePosition

object ApiDefRule : ParseRule<ApiDefNode> {
	sealed class Errors {
		data class MissingName(override val sourcePosition: SourcePosition)
			: ParseError("Api definition requires a name", sourcePosition)
	}

	override fun parse(context: Parser) : ApiDefNode {
		val start = context.expect(TokenTypes.Api)
		val typeIdentifierNode = context.attempt(TypeIdentifierRule.LValue)
			?: throw context.invocation.make(ApiDefRule.Errors.MissingName(start.position))

		val withinNode = context.attempt(WithinRule)
		var with = context.attempt(WithRule)
		var withNodes = mutableListOf<TypeIdentifierNode>()
		
		while (with != null) {
			withNodes.add(with)
			with = context.attempt(WithRule)
		}
		
		context.expect(TokenTypes.LBrace)
		
		val entityDefNodes = mutableListOf<EntityDefNode>()
		var methodDefNodes = mutableListOf<MethodDefNode>()
		var next: Token = context.peek()

		while (next.type != TokenTypes.RBrace) {
			val entity = context.attempt(TypeDefRule)
				?: context.attempt(TraitDefRule)

			if (entity == null) {
				val methodDefNode = context.attempt(MethodDefRule, true)
					?: throw Exception("Expected method signature following '(' at container level")

				methodDefNodes.add(methodDefNode)
			} else {
				entityDefNodes.add(entity)
			}

			next = context.peek()
		}

		val end = context.expect(TokenTypes.RBrace)

		// TODO - Parse
		return ApiDefNode(start, end,
			typeIdentifierNode, emptyList(), methodDefNodes, withinNode, withNodes)
	}
}