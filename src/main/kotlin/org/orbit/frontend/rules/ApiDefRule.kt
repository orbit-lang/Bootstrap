package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.core.components.Token
import org.orbit.core.components.SourcePosition
import org.orbit.frontend.components.ParseError
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ApiDefRule : ParseRule<ApiDefNode> {
	sealed class Errors {
		data class MissingName(override val sourcePosition: SourcePosition)
			: ParseError("Api definition requires a name", sourcePosition)
	}

	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expect(TokenTypes.Api)
		val typeIdentifierNode = context.attempt(TypeIdentifierRule.LValue)
			?: throw context.invocation.make(Errors.MissingName(start.position))

		val withinNode = context.attempt(WithinRule)
		var with = context.attempt(WithRule)
		val withNodes = mutableListOf<TypeIdentifierNode>()
		
		context.expect(TokenTypes.LBrace)
		
		val entityDefNodes = mutableListOf<EntityDefNode>()
		val methodDefNodes = mutableListOf<MethodDefNode>()
		val typeAliasNodes = mutableListOf<TypeAliasNode>()
		val entityConstructorNodes = mutableListOf<EntityConstructorNode>()
		var next: Token = context.peek()

		while (next.type != TokenTypes.RBrace) {
			// TODO - Allowed required method signatures in this context
			val entity = context.attemptAny(EntityParseRule.apiTopLevelRules)

			if (entity is EntityConstructorNode) {
				entityConstructorNodes.add(entity)
			} else when (entity) {
				is TypeAliasNode -> typeAliasNodes.add(entity)
				is EntityDefNode -> entityDefNodes.add(entity)
			}

			next = context.peek()
		}

		val end = context.expect(TokenTypes.RBrace)

		val requiredTypes = mutableListOf<TypeDefNode>()
		val requiredTraits = mutableListOf<TraitDefNode>()
		val standardTypes = mutableListOf<TypeDefNode>()
		val standardTraits = mutableListOf<TraitDefNode>()

		for (entityNode in entityDefNodes) {
			when (entityNode) {
				is TypeDefNode -> standardTypes.add(entityNode)
				is TraitDefNode -> standardTraits.add(entityNode)
			}
		}

		// TODO - Entity constructors in Apis
		return +ApiDefNode(start, end,
			typeIdentifierNode, requiredTypes, requiredTraits, methodDefNodes, withinNode, withNodes, standardTypes + standardTraits, entityConstructorNodes)
	}
}