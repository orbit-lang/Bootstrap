package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.backend.utils.AnyType
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.CheckNode
import org.orbit.precess.frontend.components.nodes.ExprNode
import org.orbit.precess.frontend.components.nodes.TypeExprNode

object CheckRule : ParseRule<CheckNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Check)

        context.expect(TokenTypes.LParen)

        // TODO - Add InvokeRule
        val expr = context.attemptAny(RefExprRule, TypeLookupRule, ArrowRule)
            as? ExprNode
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.Comma)

        val type = context.attemptAny(listOf(TypeLookupRule, ArrowRule))
            as? TypeExprNode
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.RParen)

        return +CheckNode(start, type.lastToken, expr, type)
    }
}