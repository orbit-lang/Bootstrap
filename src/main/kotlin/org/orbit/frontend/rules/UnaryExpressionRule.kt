package org.orbit.frontend.rules

import org.orbit.core.nodes.UnaryExpressionNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.parseTrailing
import org.orbit.frontend.phase.Parser

object UnaryExpressionRule : ValueRule<UnaryExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val operator = context.expect(TokenTypes.Operator)
        val operandExpression = context.attempt(ExpressionRule.defaultValue)
            ?: TODO("@UnaryExpressionRule:15")

        return parseTrailing(context,
            UnaryExpressionNode(operator, operandExpression.lastToken, operator.text, operandExpression)
        )
    }
}