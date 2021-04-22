package org.orbit.frontend.rules

import org.orbit.core.nodes.BinaryExpressionNode
import org.orbit.core.nodes.ExpressionNode
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes

object BinaryExpressionRule : ValueRule<BinaryExpressionNode> {
    override fun parse(context: Parser): BinaryExpressionNode {
        val start = context.peek()
        val leftExpression = context.attempt(ExpressionRule.defaultValue, true)
            ?: throw Exception("TODO")

        val operator = context.expect(TokenTypes.Operator)
        val rightExpressionRule = context.attempt(ExpressionRule.defaultValue, true)
            ?: throw Exception("TODO")

        return BinaryExpressionNode(start, rightExpressionRule.lastToken, operator.text, leftExpression, rightExpressionRule)
    }
}

class PartialExpressionRule(private val leftExpression: ExpressionNode) : ValueRule<BinaryExpressionNode> {
    override fun parse(context: Parser): BinaryExpressionNode {
        val op = context.expect(TokenTypes.Operator)
        val rightExpression = context.attempt(ExpressionRule.defaultValue)
            ?: throw Exception("TODO")

        return BinaryExpressionNode(op, rightExpression.lastToken, op.text, leftExpression, rightExpression)
    }
}