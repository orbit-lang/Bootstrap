package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.CheckNode
import org.orbit.precess.frontend.components.nodes.TermExpressionNode

object CheckRule : ParseRule<CheckNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Check)

        context.expect(TokenTypes.LParen)

        val expr = context.attemptAny(RefExprRule, TypeLookupRule, ArrowRule)
            as? TermExpressionNode<*>
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.Comma)

        val type = context.attemptAny(listOf(TypeLookupRule, ArrowRule))
            as? TermExpressionNode<*>
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.RParen)

        return +CheckNode(start, type.lastToken, expr, type)
    }
}