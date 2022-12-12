package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.SumTypeNode
import org.orbit.core.nodes.TaggedTypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TaggedTypeExpressionRule : ParseRule<TaggedTypeExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val identifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        val next = context.peek()

        if (next.type != TokenTypes.Colon) return ParseRule.Result.Failure.Rewind(collector)

        context.consume()

        val expr = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Type Expression after `:`", collector)

        return +TaggedTypeExpressionNode(identifier.firstToken, expr.lastToken, identifier, expr)
    }
}

object SumTypeRule : ParseRule<SumTypeNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val start = context.expect(TokenTypes.LParen)
        val left = context.attempt(TaggedTypeExpressionRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        val next = context.peek()

        if (next.text != "|") return ParseRule.Result.Failure.Rewind(collector)

        context.consume()

        val right = context.attempt(TaggedTypeExpressionRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Type Expression on right-hand side of `|`", collector)

        val end = context.expect(TokenTypes.RParen)

        return +SumTypeNode(start, end, left, right)
    }
}