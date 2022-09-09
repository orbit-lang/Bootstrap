package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.CaseMode
import org.orbit.core.nodes.CaseNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.precess.backend.components.parse

object CaseRule : ParseRule<CaseNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Case)
        var next = context.peek()
        val pattern = context.attempt(AnyPatternRule)
            ?: return ParseRule.Result.Failure.Throw("Expected pattern after `case...`, found ${next.text}", next)

        next = context.expectAny(TokenTypes.Assignment, TokenTypes.By, consumes = true)

        val mode = CaseMode.parse(next.text)
            ?: return ParseRule.Result.Failure.Throw("Expected Case Mode (`=` or `by`) after `case...`, found ${next.text}", next)
        val body = context.attempt(ExpressionRule.singleExpressionBodyRule)
            ?: return ParseRule.Result.Failure.Abort

        return +CaseNode(start, body.lastToken, pattern, mode, body)
    }
}