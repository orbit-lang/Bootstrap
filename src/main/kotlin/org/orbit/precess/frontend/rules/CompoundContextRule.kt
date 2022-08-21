package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.CompoundContextCallNode

object CompoundContextRule : ParseRule<CompoundContextCallNode> {
    // TODO - Simplify
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val ctx = context.attempt(ContextCallRule)
            ?: return ParseRule.Result.Failure.Abort

        val recorded = context.end()

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(recorded)

        var next = context.peek()

        if (next.type != TokenTypes.Extend) return ParseRule.Result.Failure.Rewind(recorded)

        val plus = context.expect(TokenTypes.Extend)
        val head = context.attempt(ContextCallRule)
            ?: return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken, plus))

        val rhs = mutableListOf(ctx, head)

        if (!context.hasMore) return +CompoundContextCallNode(ctx.firstToken, head.lastToken, listOf(head))

        next = context.peek()
        while (next.type == TokenTypes.Extend) {
            context.consume()
            val nextContext = context.attempt(ContextCallRule)
                ?: return ParseRule.Result.Failure.Abort

            rhs.add(nextContext)

            if (!context.hasMore) break

            next = context.peek()
        }

        return +CompoundContextCallNode(ctx.firstToken, rhs.last().lastToken, rhs)
    }
}