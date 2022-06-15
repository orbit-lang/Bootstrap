package org.orbit.frontend.rules

import org.orbit.core.nodes.UnaryExpressionNode
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.OperatorFixity
//import org.orbit.frontend.extensions.parseTrailing
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object UnaryExpressionRule : ValueRule<UnaryExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val operator = context.expect(TokenTypes.OperatorSymbol)
        val operandExpression = context.attempt(ExpressionRule.defaultValue)
            ?: TODO("@UnaryExpressionRule:15")

        return +UnaryExpressionNode(operator, operandExpression.lastToken, operator.text, operandExpression, OperatorFixity.Prefix)
    }
}