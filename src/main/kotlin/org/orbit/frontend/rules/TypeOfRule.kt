package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.IExpressionNode
import org.orbit.core.nodes.TypeOfNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TypeOfRule : ParseRule<TypeOfNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.TypeOf)
        val next = context.peek()
        val expr = context.attemptAny(listOf(GroupedExpressionRule, ExpressionRule.defaultValue))
            as? IExpressionNode
            ?: return ParseRule.Result.Failure.Throw("`typeOf` operator only works on values", next)

        return +TypeOfNode(start, expr.lastToken, expr)
    }
}