package org.orbit.frontend.rules

import org.orbit.core.nodes.ObserverNode
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes

object ObserverRule : ParseRule<ObserverNode> {
    override fun parse(context: Parser) : ObserverNode {
        val start = context.expect(TokenTypes.Observe)
        val annotation = context.attempt(PhaseAnnotationRule)
            ?: throw context.invocation.make<Parser>("Expected annotation", context.peek().position)

        val parameter = context.attempt(ParenthesisedRule(PairRule))
            ?: throw context.invocation.make<Parser>("Expected parameter", context.peek().position)

        val block = context.attempt(BlockRule(AssignmentRule))
            ?: throw context.invocation.make<Parser>("Expected block", context.peek().position)


        return ObserverNode(start, block.lastToken, annotation, parameter, block)
    }
}