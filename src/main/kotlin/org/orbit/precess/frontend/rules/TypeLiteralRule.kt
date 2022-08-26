package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.backend.components.TypeAttribute
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.TypeLiteralNode

object TypeLiteralRule : ParseRule<TypeLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val id = context.expect(TokenTypes.TypeId)

        if (!context.hasMore) return +TypeLiteralNode(id, id, id.text)

        var next = context.peek()
        val attributes = mutableListOf<TypeAttribute>()
        while (next.type == TokenTypes.TypeAttribute) {
            context.consume()
            val attribute = TypeAttribute.parse(next.text)
                ?: throw context.invocation.make<Parser>("No Type Attribute defined for symbol `${next.text}`", next)

            attributes.add(attribute)

            if (!context.hasMore) break

            next = context.peek()
        }

        return +TypeLiteralNode(id, id, id.text, attributes)
    }
}