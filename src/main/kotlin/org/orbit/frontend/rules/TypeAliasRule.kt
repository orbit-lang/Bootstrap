package org.orbit.frontend.rules

import org.orbit.core.nodes.TypeAliasNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TypeAliasRule : ParseRule<TypeAliasNode> {
    override fun parse(context: Parser): ParseRule.Result {
        return context.record {
            val start = context.expectOrNull(TokenTypes.Alias)
                ?: return@record ParseRule.Result.Failure.Rewind(emptyList())

            val source = context.attempt(TypeIdentifierRule.Naked)
                ?: return@record ParseRule.Result.Failure.Abort

            context.expect(TokenTypes.Assignment)

            val target = context.attempt(TypeExpressionRule)
                ?: return@record ParseRule.Result.Failure.Abort

            return@record +TypeAliasNode(start, target.lastToken, source, target)
        }
    }
}