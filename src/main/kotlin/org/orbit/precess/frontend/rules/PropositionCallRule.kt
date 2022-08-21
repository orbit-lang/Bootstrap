package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.PropositionCallNode

object PropositionCallRule : ParseRule<PropositionCallNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val id = context.expect(TokenTypes.TypeId)

        context.expect(TokenTypes.LParen)

        val ctx = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.RParen)

        return +PropositionCallNode(id, ctx.lastToken, id.text, ctx)
    }
}