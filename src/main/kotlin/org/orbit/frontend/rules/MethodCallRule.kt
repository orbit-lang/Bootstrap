package org.orbit.frontend.rules

import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.nodes.InstanceMethodCallNode
import org.orbit.core.nodes.MethodCallNode
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes

interface MethodCallRule<M: MethodCallNode> : ValueRule<M>

object InstanceMethodCallRule : MethodCallRule<InstanceMethodCallNode> {
    override fun parse(context: Parser): InstanceMethodCallNode {
        val instanceIdentifier = context.attempt(IdentifierRule)
            ?: throw context.invocation.make<Parser>("Expected identifier", context.peek().position)

        if (context.peek().type != TokenTypes.Dot) {
            context.rewind(listOf(instanceIdentifier.firstToken))
        }

        context.expect(TokenTypes.Dot)

        val methodIdentifier = context.attempt(IdentifierRule)
            ?: throw context.invocation.make<Parser>("Expected method name", context.peek().position)

        context.expect(TokenTypes.LParen)

        var next = context.peek()

        if (next.type == TokenTypes.RParen) {
            context.consume()
            return InstanceMethodCallNode(instanceIdentifier.firstToken, next, methodIdentifier, instanceIdentifier, emptyList())
        }

        val parameterNodes = mutableListOf<ExpressionNode>()
        while (next.type != TokenTypes.RParen) {
            val expression = context.attempt(ExpressionRule.defaultValue)
                ?: throw context.invocation.make<Parser>("Method call parameters must be expressions", next.position)

            parameterNodes.add(expression)

            next = context.peek()

            if (next.type == TokenTypes.Comma) {
                context.consume()
                next = context.peek()
            }
        }

        val last = context.expect(TokenTypes.RParen)

        return InstanceMethodCallNode(instanceIdentifier.firstToken, last, methodIdentifier, instanceIdentifier, parameterNodes)
    }
}