package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ContextNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ContextRule : ParseRule<ContextNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Context)
        val contextIdentifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Abort

        val typeVariablesRule = DelimitedRule(TokenTypes.LBracket, TokenTypes.RBracket, TypeIdentifierRule.Naked)
        val delim = context.attempt(typeVariablesRule)
            ?: return ParseRule.Result.Failure.Abort

        val clauses = mutableListOf<WhereClauseNode>()
        var next = context.peek()
        while (next.type == TokenTypes.Where) {
            val clause = context.attempt(WhereClauseRule.context)
                ?: return ParseRule.Result.Failure.Abort

            clauses.add(clause)

            next = context.peek()
        }

        return +ContextNode(start, delim.lastToken, contextIdentifier, delim.nodes, clauses)
    }
}