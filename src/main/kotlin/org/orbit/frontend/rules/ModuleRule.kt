package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes

object ModuleRule : ParseRule<ModuleNode> {
    override fun parse(context: Parser): ModuleNode {
        val start = context.expect(TokenTypes.Module)
        val typeIdentifierNode = context.attempt(TypeIdentifierRule.LValue)
            ?: throw context.invocation.make(ApiDefRule.Errors.MissingName(start.position))

        var next = context.peek()
        val implements = mutableListOf<TypeIdentifierNode>()

        if (next.type == TokenTypes.Colon) {
            context.consume()

            while (true) {
                val expr = LiteralRule(TypeIdentifierRule.RValue).parse(context)
                    //TypeIdentifierRule.RValue.parse(context)

                val impl = expr.expressionNode as? TypeIdentifierNode
                    ?: throw Exception("TODO")

                implements.add(impl)

                next = context.peek()

                if (next.type == TokenTypes.Comma) {
                    context.consume()
                    next = context.peek()
                } else if (next.type != TokenTypes.TypeIdentifier) {
                    break
                }
            }
        }

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
        next = context.peek()

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

        return ModuleNode(start, end, emptyList(), typeIdentifierNode, withinNode, withNodes, typeDefNodes, traitDefNodes, methodDefNodes)
    }
}