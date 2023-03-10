package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ForNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ForRule : ValueRule<ForNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.For)
        val iterable = context.attempt(ExpressionRule.defaultValue)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.By)

        val body = context.attempt(InvokableReferenceRule)
            ?: return ParseRule.Result.Failure.Abort

        return +ForNode(start, body.lastToken, iterable, body)
    }
}