package org.orbit.frontend.rules

import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

object MethodReferenceRule : ValueRule<MethodReferenceNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val start = context.expectOrNull(TokenTypes.TypeIdentifier)
            ?: return ParseRule.Result.Failure.Rewind()
        val recorded = context.end()

        val typeIdentifier = TypeIdentifierNode(start, start, start.text)
        val next = context.peek()

        if (next.type != TokenTypes.Colon) ParseRule.Result.Failure.Rewind(recorded)

        context.expect(TokenTypes.Colon)
        context.expect(TokenTypes.Colon)

        val identifier = context.attempt(IdentifierRule)
            ?: throw context.invocation.make<Parser>("Expected identifier", context.peek().position)

        return +MethodReferenceNode(start, identifier.lastToken, typeIdentifier, identifier)
    }
}