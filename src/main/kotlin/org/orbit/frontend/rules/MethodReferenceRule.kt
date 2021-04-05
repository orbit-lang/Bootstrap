package org.orbit.frontend.rules

import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes

object MethodReferenceRule : ValueRule<MethodReferenceNode> {
    override fun parse(context: Parser): MethodReferenceNode {
        val start = context.expect(TokenTypes.TypeIdentifier)

        val typeIdentifier = TypeIdentifierNode(start, start, start.text)

        context.expect(TokenTypes.Colon)
        context.expect(TokenTypes.Colon)

        val identifier = context.attempt(IdentifierRule)
            ?: throw context.invocation.make<Parser>("Expected identifier", context.peek().position)

        return MethodReferenceNode(start, identifier.lastToken, typeIdentifier, identifier)
    }
}