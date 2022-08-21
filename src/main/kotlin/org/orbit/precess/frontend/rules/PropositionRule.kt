package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.ExprNode
import org.orbit.precess.frontend.components.nodes.PropositionNode

object PropositionRule : ParseRule<PropositionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val id = context.expect(TokenTypes.TypeId)

        context.expect(TokenTypes.Assign)

        val eCtx = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.FatArrow)

        val body = context.attemptAny(listOf(CheckRule, PropositionCallRule, ContextLiteralRule))
            as? ExprNode
            ?: return ParseRule.Result.Failure.Abort

        return +PropositionNode(id, body.lastToken, id.text, eCtx, body)
    }
}