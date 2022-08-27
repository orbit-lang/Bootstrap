package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.backend.components.ContextOperator
import org.orbit.precess.backend.components.parse
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.*

object ModifyContextRule : ParseRule<ModifyContextNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val ctx = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken))

        val next = context.peek()

        if (next.type != TokenTypes.ContextOperator) return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken))

        val op = context.expect(TokenTypes.ContextOperator)
        val contextOperator = ContextOperator.parse(op.text)
            ?: throw context.invocation.make<Parser>("Unknown Context Operator `${op.text}`", op)

        val literal = context.attempt(AnyDeclRule)
            ?: return ParseRule.Result.Failure.Rewind(listOf(ctx.firstToken, op))

        return +ModifyContextNode(ctx.firstToken, literal.lastToken, ctx, literal, contextOperator)
    }
}