package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.TypeQueryExpressionNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TypeQueryRule : ParseRule<TypeQueryExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Query)
        val identifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.Where)

        val clause = context.attempt(AnyAttributeExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        return +TypeQueryExpressionNode(start, clause.lastToken, identifier, clause)
    }
}