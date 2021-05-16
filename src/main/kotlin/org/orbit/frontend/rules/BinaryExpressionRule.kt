package org.orbit.frontend.rules

import org.orbit.core.nodes.BinaryExpressionNode
import org.orbit.core.nodes.ExpressionNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.parseTrailing
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

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