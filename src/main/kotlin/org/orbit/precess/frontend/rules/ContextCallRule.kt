package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.ContextCallNode

object ContextCallRule : ParseRule<ContextCallNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val outer = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(listOf(outer.firstToken))

        val next = context.peek()

        if (next.type != TokenTypes.LParen) return ParseRule.Result.Failure.Rewind(listOf(outer.firstToken))

        context.expect(TokenTypes.LParen)

        val inner = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        val end = context.expect(TokenTypes.RParen)

        return +ContextCallNode(outer.firstToken, end, outer, inner)
    }
}