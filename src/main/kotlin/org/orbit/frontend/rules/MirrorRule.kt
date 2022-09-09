package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.IExpressionNode
import org.orbit.core.nodes.MirrorNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object MirrorRule : ValueRule<MirrorNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Mirror)
        val expr = context.attemptAny(listOf(TypeExpressionRule, ExpressionRule.defaultValue))
            as? IExpressionNode
            ?: return ParseRule.Result.Failure.Abort

        return +MirrorNode(start, expr.lastToken, expr)
    }
}