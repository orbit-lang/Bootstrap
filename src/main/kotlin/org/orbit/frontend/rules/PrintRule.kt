package org.orbit.frontend.rules

import org.orbit.core.nodes.PrintNode
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

object PrintRule : ParseRule<PrintNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Identifier)

        if (start.text != "print") {
            return ParseRule.Result.Failure.Rewind(listOf(start))
        }

        val expression = context.attempt(ExpressionRule.defaultValue)
            ?: throw context.invocation.make<Parser>("", context.peek())

        return +PrintNode(start, expression.lastToken, expression)
    }
}