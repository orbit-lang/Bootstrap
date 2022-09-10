package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.PanicNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object PanicRule : ValueRule<PanicNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Panic)

        if (!context.hasMore) return ParseRule.Result.Failure.Throw("Expected expression after `panic...`", start)

        val next = context.peek()
        val expr = context.attempt(ExpressionRule.defaultValue)
            ?: return ParseRule.Result.Failure.Throw("Expected expression after `panic...`", next)

        return +PanicNode(start, expr.lastToken, expr)
    }
}