package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ConstantExpressionNode
import org.orbit.core.nodes.ExpandNode
import org.orbit.core.nodes.RValueNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation
import kotlin.math.exp

object ExpandRule : ValueRule<ExpandNode>, KoinComponent {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Expand)
        val expr = context.attempt(ExpressionRule.defaultValue)
            ?: return ParseRule.Result.Failure.Abort

        return +ExpandNode(start, expr.lastToken, when (expr) {
            is RValueNode -> expr.expressionNode
            else -> expr
        })
    }
}