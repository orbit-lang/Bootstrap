package org.orbit.frontend.rules

import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.IContextExpressionNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AssignmentRule : ValueRule<AssignmentStatementNode>, WhereClauseExpressionRule<AssignmentStatementNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.peek()
        val collector = context.startCollecting()
        val identifier = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        var next = context.peek()

        if (next.type != TokenTypes.Assignment) {
            return ParseRule.Result.Failure.Rewind(collector)
        }

        context.expect(TokenTypes.Assignment)

        val value = context.attempt(ExpressionRule.defaultValue)
            ?: throw context.invocation.make<Parser>("@AssignmentRule:27", context.peek().position)

        if (!context.hasMore) return +AssignmentStatementNode(start, value.lastToken, identifier, value, null, null)

        next = context.peek()

        val contextNode: IContextExpressionNode? = when (next.type) {
            TokenTypes.Within -> context.attempt(ContextExpressionRule)
                ?: return ParseRule.Result.Failure.Abort

            else -> null
        }

        return +AssignmentStatementNode(start, value.lastToken, identifier, value, null, contextNode)
    }
}