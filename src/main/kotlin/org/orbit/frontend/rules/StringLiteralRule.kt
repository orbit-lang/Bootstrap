package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.StringLiteralNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object StringLiteralRule : ValueRule<StringLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.String)

        return +StringLiteralNode(start, start, start.text)
    }
}