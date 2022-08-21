package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.TypeLookupNode

object TypeLookupRule : ParseRule<TypeLookupNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val ctx = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken))

        val next = context.peek()

        if (next.type != TokenTypes.Dot) return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken))

        context.consume()

        val type = context.attempt(TypeLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        return +TypeLookupNode(ctx.firstToken, type.lastToken, ctx, type)
    }
}