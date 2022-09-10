package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.CaseNode
import org.orbit.core.nodes.IdentifierNode
import org.orbit.core.nodes.SelectNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object SelectRule : ParseRule<SelectNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Select)
        var next = context.peek()
        val condition = context.attempt(ExpressionRule.selectConditionRule)
            ?: return ParseRule.Result.Failure.Throw("Only the following expressions are allowed as the condition of a Select expression:\n\tMethod Call, Literal, Constructor Call", next)

        next = context.peek()

        val binding: IdentifierNode? = when (next.type) {
            TokenTypes.As -> {
                context.consume()
                context.attempt(IdentifierRule)  ?: return ParseRule.Result.Failure.Abort
            }
            else -> null
        }

        val block = context.attempt(CaseRule.toBlockRule())
            ?: return ParseRule.Result.Failure.Abort

        if (block.isEmpty) return ParseRule.Result.Failure.Throw("The body of a Select expression must not be empty", block.firstToken)

        return +SelectNode(start, block.lastToken, condition, binding, block.body as List<CaseNode>)
    }
}