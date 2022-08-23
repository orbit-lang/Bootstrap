package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.CompoundPropositionCallNode

object CompoundPropositionCallRule : ParseRule<CompoundPropositionCallNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()

        val head = context.attempt(PropositionCallRule)
            ?: return ParseRule.Result.Failure.Abort

        val recorded = context.end()

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(recorded)

        var next = context.peek()

        if (next.type != TokenTypes.And) return ParseRule.Result.Failure.Rewind(recorded)

        val props = mutableListOf(head)
        next = context.peek()
        while (context.hasMore && next.type == TokenTypes.And) {
            context.consume()
            val prop = context.attempt(PropositionCallRule)
                ?: return ParseRule.Result.Failure.Abort

            props.add(prop)

            if (!context.hasMore) break

            next = context.peek()
        }

        return +CompoundPropositionCallNode(head.firstToken, props.last().lastToken, props)
    }
}