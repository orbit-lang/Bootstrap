package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.AnonymousParameterNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AnonymousParameterRule : ParseRule<AnonymousParameterNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Dollar)
        val index = start.text.drop(1).toIntOrNull()
            ?: return ParseRule.Result.Failure.Throw("Expected number after `$`", start)

        return +AnonymousParameterNode(start, start, index)
    }
}