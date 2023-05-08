package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.SelfNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object SelfRule : ValueRule<SelfNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Self)

        return +SelfNode(start, start)
    }
}