package org.orbit.frontend.rules

import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.nodes.ExpressionNode
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.unaryPlus

object AssignmentRule : ParseRule<AssignmentStatementNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.peek()
        val identifier = context.attempt(IdentifierRule, true)!!

        val next = context.peek()

        if (next.type != TokenTypes.Assignment) {
            // TODO - Something is wrong with automatic rewinding here
            context.rewind(listOf(start))
        }

        context.expect(TokenTypes.Assignment)

        val value = context.attemptAny(*ExpressionRule.defaultValue.valueRules, throwOnNull = true)
            as? ExpressionNode
            ?: throw context.invocation.make<Parser>("@AssignmentRule:27", context.peek().position)

        return +AssignmentStatementNode(start, value.lastToken, identifier, value)
    }
}