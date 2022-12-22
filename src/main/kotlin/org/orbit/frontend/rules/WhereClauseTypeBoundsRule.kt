package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ITypeBoundsOperator
import org.orbit.core.nodes.WhereClauseTypeBoundsExpressionNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object WhereClauseTypeBoundsRule : WhereClauseExpressionRule<WhereClauseTypeBoundsExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val sourceType = context.attempt(TypeExpressionRule)
            ?: TODO("@TypeProjectionRule:17")

        val op = context.expectAny(TokenTypes.OperatorSymbol, TokenTypes.Assignment, TokenTypes.Colon, TokenTypes.Identifier, consumes = true)
        val boundsType = ITypeBoundsOperator.valueOf(op)

        val targetType = context.attempt(TypeExpressionRule)
            ?: TODO("@TypeProjectionRule:24")

        return +WhereClauseTypeBoundsExpressionNode(
            sourceType.firstToken, targetType.lastToken, boundsType, sourceType, targetType
        )
    }
}