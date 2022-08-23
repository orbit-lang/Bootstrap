package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.DumpNode

object DumpRule : ParseRule<DumpNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Dump)

        context.expect(TokenTypes.LParen)

        val ctx = context.expect(TokenTypes.Delta)

        context.expect(TokenTypes.RParen)

        return +DumpNode(start, ctx)
    }
}