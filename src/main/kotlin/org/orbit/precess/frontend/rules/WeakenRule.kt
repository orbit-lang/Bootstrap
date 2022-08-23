package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.BindingLiteralNode
import org.orbit.precess.frontend.components.nodes.RefLiteralNode
import org.orbit.precess.frontend.components.nodes.TypeLiteralNode
import org.orbit.precess.frontend.components.nodes.WeakenNode

object WeakenRule : ParseRule<WeakenNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val ctx = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken))

        val next = context.peek()

        if (next.type != TokenTypes.Extend) return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken))

        val plus = context.expect(TokenTypes.Extend)

        val literal = context.attemptAny(listOf(TypeLiteralRule, BindingLiteralRule))
            ?: return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken, plus))

        return when (literal) {
            is TypeLiteralNode -> +WeakenNode(ctx.firstToken, literal.lastToken, ctx, literal)
            is BindingLiteralNode -> +WeakenNode(ctx.firstToken, literal.lastToken, ctx, literal)
            else -> TODO()
        }
    }
}