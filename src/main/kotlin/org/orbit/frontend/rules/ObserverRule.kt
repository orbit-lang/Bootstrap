package org.orbit.frontend.rules

import org.orbit.core.nodes.ObserverNode
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.unaryPlus

object ObserverRule : ParseRule<ObserverNode> {
    override fun parse(context: Parser) : ParseRule.Result {
        val start = context.expect(TokenTypes.Observe)
        val identifier = context.attempt(MethodReferenceRule)
            ?: throw context.invocation.make<Parser>("Expected identifier for observer", context.peek().position)

        return +ObserverNode(start, identifier.lastToken, identifier)
    }
}