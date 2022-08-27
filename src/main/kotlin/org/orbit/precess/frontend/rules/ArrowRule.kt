package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.ArrowNode

object ArrowRule : ParseRule<ArrowNode> {
    override fun parse(context: Parser): ParseRule.Result {
        if (context.peek().type != TokenTypes.LParen) return ParseRule.Result.Failure.Abort

        context.mark()
        val start = context.expect(TokenTypes.LParen)
        val domain = context.attempt(AnyTypeExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        val next = context.peek()

        if (next.type != TokenTypes.RParen) return ParseRule.Result.Failure.Rewind(context.end())

        context.expect(TokenTypes.RParen)
        context.expect(TokenTypes.Arrow)

        val codomain = context.attempt(AnyTypeExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        return +ArrowNode(start, codomain.lastToken, domain, codomain)
    }
}