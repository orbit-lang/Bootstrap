package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.UnboxNode

object UnboxRule : ParseRule<UnboxNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Unbox)
        val term = context.attempt(AnyTermExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        return +UnboxNode(start, term.lastToken, term)
    }
}