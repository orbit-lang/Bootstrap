package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeProjectionNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object TypeProjectionRule : ParseRule<TypeProjectionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Projection)

        val typeIdentifier = context.attempt(TypeExpressionRule)
            ?: throw invocation.make<Parser>("Expected type identifier after `type projection`", context.peek())

        context.expect(TokenTypes.Colon)

        val traitIdentifierRule = context.attempt(TypeExpressionRule)
            ?: throw invocation.make<Parser>("Expected trait identifier after `type projection ${typeIdentifier.value} :`", context.peek())

        var next = context.peek()
        val whereClauses = mutableListOf<WhereClauseNode>()
        while (next.type == TokenTypes.Where) {
            val whereClause = context.attempt(WhereClauseRule.typeProjection)
                ?: throw invocation.make<Parser>("Expected where clause", context.peek())

            whereClauses += whereClause
            next = context.peek()
        }

        // TODO - Body (properties & methods)

        return +TypeProjectionNode(start, traitIdentifierRule.lastToken, typeIdentifier, traitIdentifierRule, whereClauses)
    }
}