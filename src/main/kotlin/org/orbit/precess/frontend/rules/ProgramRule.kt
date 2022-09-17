package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.nodes.ProgramNode
import org.orbit.precess.frontend.components.nodes.IStatementNode

object ProgramRule : ParseRule<ProgramNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val statements = mutableListOf<IStatementNode>()
        while (context.hasMore) {
            val statement = context.attemptAny(listOf(PropositionRule, RunRule))
                ?: return ParseRule.Result.Failure.Throw("No statement", context.peek())

            if (statement !is IStatementNode) {
                return ParseRule.Result.Failure.Throw("No statement $statement", context.peek())
            }

            statements.add(statement)
        }

        // TODO - Throw via invocation
        if (statements.isEmpty()) throw Exception("Nothing to do")

        return +ProgramNode(statements.first().firstToken, statements.last().lastToken, statements)
    }
}