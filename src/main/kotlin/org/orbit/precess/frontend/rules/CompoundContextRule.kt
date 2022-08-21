package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.CompoundContextNode

object CompoundContextRule : ParseRule<CompoundContextNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val ctx = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken))

        val plus = context.expect(TokenTypes.Extend)
        val head = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken, plus))

        val rhs = mutableListOf(head)

        if (!context.hasMore) return +CompoundContextNode(ctx.firstToken, head.lastToken, ctx, listOf(head))

        var next = context.peek()
        while (next.type == TokenTypes.Extend) {
            context.consume()
            val nextContext = context.attempt(ContextLiteralRule)
                ?: return ParseRule.Result.Failure.Abort

            rhs.add(nextContext)

            if (!context.hasMore) break

            next = context.peek()
        }

        return +CompoundContextNode(ctx.firstToken, rhs.last().lastToken, ctx, rhs)
    }
}