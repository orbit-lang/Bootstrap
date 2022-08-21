package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.RefLookupNode

object RefExprRule : ParseRule<RefLookupNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val ctx = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken))

        var next = context.peek()

        if (next.type != TokenTypes.Dot) return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken))

        context.expect(TokenTypes.Dot)

        val recorded = context.end()

        next = context.peek()

        if (next.type != TokenTypes.RefId) return ParseRule.Result.Failure.Rewind(recorded)

        val ref = context.attempt(RefLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        return +RefLookupNode(ctx.firstToken, ref.lastToken, ctx, ref)
    }
}