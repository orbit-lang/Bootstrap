package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.ExistsNode
import org.orbit.precess.frontend.components.nodes.PropositionExpressionNode

object ExistsRule : ParseRule<ExistsNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Exists)
        val decl = context.attempt(AnyDeclRule)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.In)

        val expr = context.attemptAny(listOf(ExistsRule, CheckRule))
            as? PropositionExpressionNode
            ?: return ParseRule.Result.Failure.Abort

        return +ExistsNode(start, expr.lastToken, decl, expr)
    }
}