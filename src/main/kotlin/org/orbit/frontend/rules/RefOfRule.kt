package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.RefOfNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object RefOfRule : ParseRule<RefOfNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.RefOf)
        val next = context.peek()
        val identifier = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Throw("Expected identifier after `refOf`", next)

        return +RefOfNode(start, identifier.lastToken, identifier)
    }
}