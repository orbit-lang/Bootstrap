package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.AttributeInvocationNode
import org.orbit.core.nodes.AttributeOperator
import org.orbit.core.nodes.CompoundAttributeExpressionNode
import org.orbit.core.nodes.IAttributeExpressionNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AttributeInvocationRule : ParseRule<AttributeInvocationNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val expr = context.attempt(TypeLambdaInvocationRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        var next = context.peek()
        var lhs: IAttributeExpressionNode =
            AttributeInvocationNode(expr.firstToken, expr.lastToken, expr.typeIdentifierNode, expr.arguments)
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