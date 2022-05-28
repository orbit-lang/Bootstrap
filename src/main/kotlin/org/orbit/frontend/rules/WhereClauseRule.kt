package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.WhereClauseExpressionNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.WhereClauseByExpressionNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object WhereClauseByRule : WhereClauseExpressionRule<WhereClauseByExpressionNode>, KoinComponent {
    private val invocation: Invocation by inject()
    
    override fun parse(context: Parser): ParseRule.Result {
        val identifierNode = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Abort

        var next = context.peek()

        if (next.type != TokenTypes.By) return ParseRule.Result.Failure.Rewind(listOf(identifierNode.firstToken))

        context.expect(TokenTypes.By)

        next = context.peek()
        
        val lambdaNode = context.attempt(LambdaLiteralRule)
            ?: throw invocation.make<Parser>("Expected lambda literal after `by` keyword in Where Clause expression", next)
        
        return +WhereClauseByExpressionNode(identifierNode.firstToken, lambdaNode.lastToken, identifierNode, lambdaNode)
    }
}

class WhereClauseRule(private val whereExpressionRules: List<WhereClauseExpressionRule<*>>) : ParseRule<WhereClauseNode>, KoinComponent {
    companion object {
        val typeProjection = WhereClauseRule(listOf(WhereClauseByRule, AssignmentRule))
        val extension = WhereClauseRule(listOf(WhereClauseTypeBoundsRule))
        val context = WhereClauseRule(listOf(WhereClauseTypeBoundsRule))
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