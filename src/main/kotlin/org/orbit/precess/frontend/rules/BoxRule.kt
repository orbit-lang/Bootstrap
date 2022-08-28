package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.BoxNode

object BoxRule : ParseRule<BoxNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Box)
        val term = context.attempt(AnyTermExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        return +BoxNode(start, term.lastToken, term)
    }
}