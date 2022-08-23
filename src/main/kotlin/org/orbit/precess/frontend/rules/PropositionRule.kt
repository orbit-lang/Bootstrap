package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.PropositionExpressionNode
import org.orbit.precess.frontend.components.nodes.PropositionNode
import org.orbit.precess.frontend.components.nodes.PropositionStatementNode

object PropositionRule : ParseRule<PropositionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val id = context.expect(TokenTypes.TypeId)

        context.expect(TokenTypes.FatArrow)

        val body = context.attemptAny(listOf(WeakenRule, CheckRule, CompoundPropositionCallRule, PropositionCallRule, DumpRule, ContextLiteralRule))
            as? PropositionExpressionNode
            ?: return ParseRule.Result.Failure.Abort

        return +PropositionNode(id, body.lastToken, id.text, body)
    }
}