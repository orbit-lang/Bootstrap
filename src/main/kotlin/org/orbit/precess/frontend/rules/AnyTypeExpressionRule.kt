package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.backend.components.TypeOperator
import org.orbit.precess.backend.components.parse
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.*

private object EntityRule : ParseRule<EntityTypeExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Delta)

        context.expect(TokenTypes.Dot)

        val id = context.expect(TokenTypes.TypeId)

        return +EntityTypeExpressionNode(start, id, id.text)
    }
}

private object AlgebraicTypeRule : ParseRule<AlgebraicTypeExpressionNode<*>> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.LParen)
        val leftType = context.attempt(AnyTypeExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        val op = context.expect(TokenTypes.TypeOperator)
        val typeOperator = TypeOperator.parse(op.text)
            ?: throw context.invocation.make<Parser>("Unknown Type Operator `${op.text}`", op)

        val rightType = context.attempt(AnyTypeExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        val end = context.expect(TokenTypes.RParen)

        return +when (typeOperator) {
            TypeOperator.Product -> ProductTypeExpressionNode(start, end, leftType, rightType)
            TypeOperator.Sum -> SumTypeExpressionNode(start, end, leftType, rightType)
        }
    }
}

object AnyTypeExpressionRule : ParseRule<TypeExpressionNode<*>> {
    override fun parse(context: Parser): ParseRule.Result {
        val typeExpr = context.attemptAny(listOf(ArrowRule, AlgebraicTypeRule, EntityRule))
            as? TypeExpressionNode<*>
            ?: return ParseRule.Result.Failure.Abort

        return +typeExpr
    }
}