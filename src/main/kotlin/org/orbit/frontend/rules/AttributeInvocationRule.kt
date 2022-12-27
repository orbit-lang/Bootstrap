package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.orbit.core.Path
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AttributeMetaTypeExpressionRule : ParseRule<AttributeMetaTypeExpressionNode>, KoinComponent {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val start = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        val path = Path(start.value)

        if (path !in listOf(Path.any, Path.never)) {
            return ParseRule.Result.Failure.Rewind(collector)
        }

        return +AttributeMetaTypeExpressionNode(start.firstToken, start.lastToken, start)
    }
}

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