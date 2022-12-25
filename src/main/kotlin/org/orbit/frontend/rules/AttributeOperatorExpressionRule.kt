package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AttributeOperatorExpressionRule : ParseRule<AttributeOperatorExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val lExpr = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        val op = context.expectAny(
            TokenTypes.OperatorSymbol,
            TokenTypes.Assignment,
            TokenTypes.Colon,
            TokenTypes.Identifier, consumes = true)
        val boundsType = ITypeBoundsOperator.valueOf(op)
        val rExpr = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Throw(
                "Expected Type Expression on right-hand side of Attribute Operator Expression",
                collector.getCollectedTokens().last()
            )

        var next = context.peek()
        var lhs: IAttributeExpressionNode =
            AttributeOperatorExpressionNode(lExpr.firstToken, rExpr.lastToken, boundsType, lExpr, rExpr)
        while (next.type == TokenTypes.OperatorSymbol) {
            val opToken = context.expect(TokenTypes.OperatorSymbol)
            val attrOp = AttributeOperator.valueOf(opToken)
            val rhs = context.attempt(AnyAttributeExpressionRule)
                ?: return ParseRule.Result.Failure.Abort

            lhs = CompoundAttributeExpressionNode(lhs.firstToken, rhs.lastToken, attrOp, lhs, rhs)

            next = context.peek()
        }

        return +lhs
    }
}