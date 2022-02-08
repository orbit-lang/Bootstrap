package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.WhereClauseExpressionNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

class WhereClauseRule(private val whereExpressionRules: List<WhereClauseExpressionRule<*>>) : ParseRule<WhereClauseNode>,
    KoinComponent {
    companion object {
        val typeProjection = WhereClauseRule(listOf(AssignmentRule))
        val extension = WhereClauseRule(listOf(WhereClauseTypeBoundsRule))
    }

    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Where)
        val statement = context.attemptAny(whereExpressionRules)
            as? WhereClauseExpressionNode
            ?: throw invocation.make<Parser>("Expected where statement after `where`", start)

        return +WhereClauseNode(start, statement.lastToken, statement)
    }
}