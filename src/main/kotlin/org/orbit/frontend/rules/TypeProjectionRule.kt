package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeProjectionNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation
import org.orbit.util.error

object WhereClauseRule : ParseRule<WhereClauseNode>, KoinComponent {
    private val invocation: Invocation by inject()
    private val whereStatementRules = listOf(AssignmentRule)

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Where)
        val statement = context.attemptAny(whereStatementRules)
            ?: throw invocation.make<Parser>("Expected where statement after `where`", start)

        return +WhereClauseNode(start, statement.lastToken, statement)
    }
}

object TypeProjectionRule : ParseRule<TypeProjectionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Type)
        var next = context.peek()

        if (next.type != TokenTypes.Projection) return ParseRule.Result.Failure.Rewind(listOf(start))

        context.consume()

        val typeIdentifier = context.attempt(TypeExpressionRule)
            ?: throw invocation.make<Parser>("Expected type identifier after `type projection`", context.peek())

        context.expect(TokenTypes.Colon)

        val traitIdentifierRule = context.attempt(TypeExpressionRule)
            ?: throw invocation.make<Parser>("Expected trait identifier after `type projection ${typeIdentifier.value} :`", context.peek())

        next = context.peek()
        val whereClauses = mutableListOf<WhereClauseNode>()
        while (next.type == TokenTypes.Where) {
            val whereClause = context.attempt(WhereClauseRule)
                ?: throw invocation.make<Parser>("Expected where clause", context.peek())

            whereClauses += whereClause
            next = context.peek()
        }

        // TODO - Body (properties & methods)

        return +TypeProjectionNode(start, traitIdentifierRule.lastToken, typeIdentifier, traitIdentifierRule, whereClauses)
    }
}