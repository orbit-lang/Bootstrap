package org.orbit.frontend.rules

import org.orbit.core.nodes.ConstructorNode
import org.orbit.core.nodes.ExpressionNode
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes

class ConstructorRule : ValueRule<ConstructorNode> {
    override fun parse(context: Parser) : ConstructorNode {
        val typeIdentifier = context.attempt(TypeIdentifierRule.RValue)
            ?: throw context.invocation.make<Parser>("TODO", context.peek().position)

        context.expect(TokenTypes.LParen)

        var next = context.peek()

        if (next.type == TokenTypes.RParen) {
            val last = context.consume()

            return ConstructorNode(typeIdentifier.firstToken, last, typeIdentifier, emptyList())
        }

        val parameterNodes = mutableListOf<ExpressionNode>()
        while (next.type != TokenTypes.RParen) {
            val expressionNode = context.attempt(ExpressionRule.defaultValue)
                ?: throw context.invocation.make<Parser>("TODO", context.peek().position)

            parameterNodes.add(expressionNode)

            next = context.peek()

            if (next.type == TokenTypes.Comma) {
                context.consume()
                next = context.peek()
            }
        }

        val last = context.expect(TokenTypes.RParen)

        return ConstructorNode(typeIdentifier.firstToken, last, typeIdentifier, parameterNodes)
    }
}