package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.*

object WeakenRule : ParseRule<WeakenNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val ctx = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken))

        val next = context.peek()

        if (next.type != TokenTypes.Extend) return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken))

        val plus = context.expect(TokenTypes.Extend)

        val literal = context.attempt(AnyDeclRule)
            ?: return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken, plus))

        return +WeakenNode(ctx.firstToken, literal.lastToken, ctx, literal)
    }
}