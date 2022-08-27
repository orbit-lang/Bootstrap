package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.CompoundPropositionExpressionNode
import org.orbit.precess.frontend.components.nodes.PropositionExpressionNode
import org.orbit.precess.frontend.components.nodes.PropositionNode

private object AnyPropositionExpressionRule : ParseRule<PropositionExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val body = context.attemptAny(listOf(ModifyContextRule, CheckRule, CompoundPropositionCallRule, PropositionCallRule, DumpRule, ContextLiteralRule))
                as? PropositionExpressionNode
            ?: return ParseRule.Result.Failure.Abort

        return +body
    }
}

object PropositionRule : ParseRule<PropositionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val id = context.expect(TokenTypes.TypeId)

        context.expect(TokenTypes.FatArrow)

        val body = context.attempt(AnyPropositionExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        if (!context.hasMore) return +PropositionNode(id, body.lastToken, id.text, body)

        var next = context.peek()
        val props = mutableListOf(body)
        while (next.type == TokenTypes.FatArrow) {
            context.consume()
            val prop = context.attempt(AnyPropositionExpressionRule)
                ?: return ParseRule.Result.Failure.Abort

            props.add(prop)

            if (!context.hasMore) break

            next = context.peek()
        }

        // Save ourselves the extra work of going through the CompoundPropositionExpressionNode reduction
        if (props.count() == 1) return +PropositionNode(id, body.lastToken, id.text, body)

        return +PropositionNode(id, body.lastToken, id.text, CompoundPropositionExpressionNode(body.firstToken, props.lastOrNull()?.lastToken ?: next, props))
    }
}