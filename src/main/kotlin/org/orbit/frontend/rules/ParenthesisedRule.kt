package org.orbit.frontend.rules

import org.orbit.core.nodes.Node
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.components.TokenTypes

class ParenthesisedRule<N: Node>(private val innerRule: ParseRule<N>) : ParseRule<N> {
    override fun parse(context: Parser): ParseRule.Result {
        context.expect(TokenTypes.LParen)

        val node = innerRule.execute(context)

        context.expect(TokenTypes.RParen)

        return node
    }
}