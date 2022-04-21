package org.orbit.frontend.rules

import org.orbit.core.nodes.DeferNode
import org.orbit.core.nodes.IdentifierNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object DeferRule : ParseRule<DeferNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Defer)
        val next = context.peek()

        var returnValueIdentifier: IdentifierNode? = null
        if (next.type == TokenTypes.LParen) {
            context.consume()
            // Now expecting a single identifier for the named return value
            returnValueIdentifier = context.attempt(IdentifierRule)

            if (returnValueIdentifier == null) {
                throw context.invocation.make<Parser>("Defer statement expects an identifier after left paren", next)
            }

            context.expect(TokenTypes.RParen)
        }

        // Defer statements are not allowed to return
        val blockNode = context.attempt(BlockRule(PrintRule, PrintRule, AssignmentRule))
            ?: throw context.invocation.make<Parser>("Defer statement must be followed by a block", context.peek())

        return +DeferNode(start, blockNode.lastToken, returnValueIdentifier, blockNode)
    }
}