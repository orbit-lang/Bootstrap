package org.orbit.frontend.rules

import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser

object AssignmentRule : ParseRule<AssignmentStatementNode> {
    override fun parse(context: Parser): AssignmentStatementNode {
        val start = context.peek()
        val identifier = context.attempt(IdentifierRule, true)!!

        context.expect(TokenTypes.Assignment)

        val value = context.attempt(ExpressionRule.defaultValue, true)!!

        return AssignmentStatementNode(start, value.lastToken, identifier, value)
    }
}