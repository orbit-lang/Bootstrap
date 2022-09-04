package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ConstructorInvocationNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ConstructorInvocationRule : ValueRule<ConstructorInvocationNode> {
    override fun parse(context: Parser) : ParseRule.Result {
        context.mark()
        val typeIdentifier = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Rewind()
        val recorded = context.end()
        val next = context.peek()

        if (next.type != TokenTypes.LParen) return ParseRule.Result.Failure.Rewind(recorded)

        val delim = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, ExpressionRule.defaultValue)
        val delimResult = context.attempt(delim)
            ?: return ParseRule.Result.Failure.Abort

        return +ConstructorInvocationNode(typeIdentifier.firstToken, delimResult.lastToken, typeIdentifier, delimResult.nodes)
    }
}