package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.AttributeArrowNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AttributeArrowRule : ParseRule<AttributeArrowNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val delimRule = DelimitedRule(innerRule = TypeIdentifierRule.Naked)
        val delim = context.attempt(delimRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        if (!context.hasAtLeast(2)) return ParseRule.Result.Failure.Abort

        val next = context.peek()

        if (next.type != TokenTypes.Assignment) {
            return ParseRule.Result.Failure.Rewind(collector)
        }

        context.expect(TokenTypes.Assignment)
        context.expect(TokenTypes.RAngle)

        val parameters = delim.nodes
        val constraint = context.attempt(AnyAttributeExpressionRule)
            ?: return ParseRule.Result.Failure.Throw(
                "Expected Attribute expression on right-hand side of Attribute Arrow",
                delim.lastToken
            )

        return +AttributeArrowNode(delim.firstToken, constraint.lastToken, parameters, constraint)
    }
}