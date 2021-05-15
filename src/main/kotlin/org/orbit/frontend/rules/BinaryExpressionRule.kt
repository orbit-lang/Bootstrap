package org.orbit.frontend.rules

import org.orbit.core.nodes.BinaryExpressionNode
import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.nodes.UnaryExpressionNode
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.unaryPlus

object UnaryExpressionRule : ValueRule<UnaryExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val operator = context.expect(TokenTypes.Operator)
        val operandExpression = context.attempt(ExpressionRule.defaultValue)
            ?: TODO("@UnaryExpressionRule:15")

        return parseTrailing(context, UnaryExpressionNode(operator, operandExpression.lastToken, operator.text, operandExpression))
    }
}

object BinaryExpressionRule : ValueRule<BinaryExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.peek()
        val leftExpression = context.attempt(ExpressionRule.defaultValue, true)
            ?: TODO("@BinaryExpressionRule:12")

        val operator = context.expect(TokenTypes.Operator)
        val rightExpressionRule = context.attempt(ExpressionRule.defaultValue, true)
            ?: TODO("@BinaryExpressionRule:16")

        return +BinaryExpressionNode(start, rightExpressionRule.lastToken, operator.text, leftExpression, rightExpressionRule)
    }
}

class PartialExpressionRule(private val leftExpression: ExpressionNode) : ValueRule<BinaryExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val op = context.expect(TokenTypes.Operator)
        val rightExpression = context.attempt(ExpressionRule.defaultValue)
            ?: TODO("@BinaryExpressionRule:26")

        return parseTrailing(context, BinaryExpressionNode(op, rightExpression.lastToken, op.text, leftExpression, rightExpression))
    }
}