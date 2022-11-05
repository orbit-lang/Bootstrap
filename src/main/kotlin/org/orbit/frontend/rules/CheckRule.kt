package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.CheckNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object CheckRule : ParseRule<CheckNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Check)
        val left = context.attempt(ExpressionRule.defaultValue)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.Comma)

        val right = context.attempt(ExpressionRule.defaultValue)
            ?: return ParseRule.Result.Failure.Abort

        return +CheckNode(start, right.lastToken, left, right)
    }
}