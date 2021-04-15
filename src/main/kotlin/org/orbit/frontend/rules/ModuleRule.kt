package org.orbit.frontend.rules

import org.orbit.core.Token
import org.orbit.core.nodes.*
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.PrefixPhaseAnnotatedParseRule
import org.orbit.frontend.TokenTypes

data class PhaseAnnotationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val annotationIdentifierNode: TypeIdentifierNode) : Node(firstToken, lastToken) {

    override fun getChildren(): List<Node> {
        return listOf(annotationIdentifierNode)
    }
}

object PhaseAnnotationRule : ParseRule<PhaseAnnotationNode> {
    override fun parse(context: Parser): PhaseAnnotationNode {
        val start = context.expect(TokenTypes.Annotation)
        val annotationIdentifierNode = context.attempt(TypeIdentifierRule.LValue)
            // TODO - Rename ApiDefRule
            ?: throw context.invocation.make(ApiDefRule.Errors.MissingName(start.position))

        // TODO - Parse annotation parameters

        return PhaseAnnotationNode(start, annotationIdentifierNode.lastToken, annotationIdentifierNode)
    }
}

object ModuleRule : PrefixPhaseAnnotatedParseRule<ModuleNode> {
    override fun parse(context: Parser): ModuleNode {
        val start = context.expect(TokenTypes.Module)
        val typeIdentifierNode = context.attempt(TypeIdentifierRule.LValue)
            ?: throw context.invocation.make(ApiDefRule.Errors.MissingName(start.position))

        var next = context.peek()
        val implements = mutableListOf<TypeIdentifierNode>()

        if (next.type == TokenTypes.Colon) {
            context.consume()

            while (true) {
                val expr = LiteralRule(TypeIdentifierRule.RValue).execute(context)
                    //TypeIdentifierRule.RValue.execute(context)

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

        next = context.peek()

        var withinNode: TypeIdentifierNode? = null
        if (next.type == TokenTypes.Within) {
            withinNode = context.attempt(WithinRule)
        }

        next = context.peek()

        val withNodes = mutableListOf<TypeIdentifierNode>()
        if (next.type == TokenTypes.With) {
            var with = context.attempt(WithRule)

            while (with != null) {
                withNodes.add(with)
                with = context.attempt(WithRule)
            }
        }

        context.expect(TokenTypes.LBrace)

        val entityDefNodes = mutableListOf<EntityDefNode>()
        val methodDefNodes = mutableListOf<MethodDefNode>()

        next = context.peek()

        while (next.type != TokenTypes.RBrace) {
            when (next.type) {
                TokenTypes.LParen -> {
                    val methodDefNode = context.attempt(MethodDefRule, true)
                        ?: throw Exception("Expected method signature following '(' at container level")

                    methodDefNodes.add(methodDefNode)
                }

                TokenTypes.Type, TokenTypes.Trait -> {
                    val entity = context.attempt(TypeDefRule)
                        ?: context.attempt(TraitDefRule)

                    entityDefNodes.add(entity!!)
                }

                else -> throw RuntimeException("TODO")
            }

            next = context.peek()
        }

        val end = context.expect(TokenTypes.RBrace)

        return ModuleNode(start, end, emptyList(), typeIdentifierNode, withinNode, withNodes, entityDefNodes, methodDefNodes)
    }
}