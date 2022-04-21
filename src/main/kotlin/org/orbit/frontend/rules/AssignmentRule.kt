package org.orbit.frontend.rules

import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AssignmentRule : ParseRule<AssignmentStatementNode>, WhereClauseExpressionRule<AssignmentStatementNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.peek()
        val identifier = context.attempt(IdentifierRule, true)!!
        val typeAnnotationNode = context.attempt(TypeExpressionRule)

        val next = context.peek()

        if (next.type != TokenTypes.Assignment) {
            return ParseRule.Result.Failure.Rewind(listOf(start))
        }

        context.expect(TokenTypes.Assignment)

        val value = context.attemptAny(*ExpressionRule.defaultValue.valueRules, throwOnNull = true)
            as? ExpressionNode
            ?: throw context.invocation.make<Parser>("@AssignmentRule:27", context.peek().position)

        return +AssignmentStatementNode(start, value.lastToken, identifier, value, typeAnnotationNode)
    }
}