package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.ContextExprNode
import org.orbit.precess.frontend.components.nodes.ContextLetNode

object ContextLetRule : ParseRule<ContextLetNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val nCtx = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.Assign)

        val eCtx = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.FatArrow)

        val expr = context.attemptAny(listOf(WeakenRule, CompoundContextRule, ContextLiteralRule))
            as? ContextExprNode
            ?: return ParseRule.Result.Failure.Abort

        return +ContextLetNode(nCtx.firstToken, expr.lastToken, nCtx, eCtx, expr)
    }
}