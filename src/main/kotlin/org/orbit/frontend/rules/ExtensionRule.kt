package org.orbit.frontend.rules

import org.orbit.core.nodes.ExtensionNode
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ExtensionRule : ParseRule<ExtensionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Extension)
        val targetType = context.attempt(TypeExpressionRule)
            ?: TODO("@ExtensionRule:14")

        var next = context.peek()

        val whereClauses = mutableListOf<WhereClauseNode>()
        while (next.type == TokenTypes.Where) {
            val whereClause = context.attempt(WhereClauseRule.extension)
                ?: TODO("@ExtensionRule:21")

            whereClauses.add(whereClause)
            next = context.peek()
        }

        val body = context.attempt(BlockRule(MethodDefRule))
            ?: TODO("@ExtensionRule:28")

        return +ExtensionNode(start, body.lastToken, targetType, body.body as List<MethodDefNode>, whereClauses)
    }
}