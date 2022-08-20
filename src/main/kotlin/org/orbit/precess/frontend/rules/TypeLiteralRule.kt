package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.TypeLiteralNode

object TypeLiteralRule : ParseRule<TypeLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val id = context.expect(TokenTypes.TypeId)

        return +TypeLiteralNode(id, id, id.text)
    }
}