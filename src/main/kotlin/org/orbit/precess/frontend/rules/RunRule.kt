package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.RunNode

object RunRule : ParseRule<RunNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Run)

        val prop = context.attempt(PropositionCallRule)
            ?: return ParseRule.Result.Failure.Abort

        return +RunNode(start, prop.lastToken, prop)
    }
}