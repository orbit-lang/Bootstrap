package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ExpandNode
import org.orbit.core.nodes.IConstantExpressionNode
import org.orbit.core.nodes.RValueNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ExpandRule : ValueRule<ExpandNode>, KoinComponent {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.LExpand)

        val expr = context.attempt(ExpressionRule.defaultValue)
            ?: return ParseRule.Result.Failure.Abort

        val end = context.expect(TokenTypes.RBrace)

        return +ExpandNode(start, end, when (expr) {
            is RValueNode -> (expr.expressionNode as? IConstantExpressionNode) ?: return ParseRule.Result.Failure.Abort
            else -> (expr as? IConstantExpressionNode) ?: return ParseRule.Result.Failure.Abort
        })
    }
}