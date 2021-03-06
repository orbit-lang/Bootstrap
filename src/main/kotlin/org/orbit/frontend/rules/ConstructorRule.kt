package org.orbit.frontend.rules

import org.orbit.core.nodes.ConstructorNode
import org.orbit.core.nodes.ExpressionNode
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.parseTrailing

class ConstructorRule : ValueRule<ConstructorNode> {
    override fun parse(context: Parser) : ParseRule.Result {
        val typeIdentifier = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Rewind()

        context.expectOrNull(TokenTypes.LParen)
            ?: return ParseRule.Result.Failure.Rewind(listOf(typeIdentifier.firstToken))

        var next = context.peek()

        if (next.type == TokenTypes.RParen) {
            val last = context.consume()

            return parseTrailing(context, ConstructorNode(typeIdentifier.firstToken, last, typeIdentifier, emptyList()))
        }

        val parameterNodes = mutableListOf<ExpressionNode>()
        while (next.type != TokenTypes.RParen) {
            val expressionNode = context.attemptAny(ExpressionRule.defaultValue)
                as? ExpressionNode
                ?: return ParseRule.Result.Failure.Abort

            parameterNodes.add(expressionNode)

            next = context.peek()

            if (next.type == TokenTypes.Comma) {
                context.consume()
                next = context.peek()
            }
        }

        val last = context.expect(TokenTypes.RParen)

        return parseTrailing(context, ConstructorNode(typeIdentifier.firstToken, last, typeIdentifier, parameterNodes))
    }
}