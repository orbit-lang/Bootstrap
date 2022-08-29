package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.SafeNode

object SafeRule : ParseRule<SafeNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Safe)
        val term = context.attempt(AnyTermExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        return +SafeNode(start, term.lastToken, term)
    }
}