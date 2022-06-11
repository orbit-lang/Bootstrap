package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ConstructorNode
import org.orbit.frontend.extensions.parseTrailing
import org.orbit.frontend.phase.Parser

object ConstructorRule : ValueRule<ConstructorNode> {
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

        return parseTrailing(context, ConstructorNode(typeIdentifier.firstToken, delimResult.lastToken, typeIdentifier, delimResult.nodes))
    }
}