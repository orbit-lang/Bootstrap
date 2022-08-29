package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.TypeAliasNode

object TypeAliasRule : ParseRule<TypeAliasNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.TypeId)

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(listOf(start))

        val next = context.peek()

        if (next.type != TokenTypes.Bind) return ParseRule.Result.Failure.Rewind(listOf(start))

        context.expect(TokenTypes.Bind)

        val type = context.attempt(AnyTermExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        return +TypeAliasNode(start, type.lastToken, start.text, type)
    }
}