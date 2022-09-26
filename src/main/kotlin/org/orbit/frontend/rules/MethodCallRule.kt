package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
//import org.orbit.frontend.extensions.parseTrailing
import org.orbit.frontend.extensions.unaryPlus

interface CallRule<N: InvokableNode> : ValueRule<N>

object ReferenceCallRule : CallRule<ReferenceCallNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.expect(TokenTypes.Invoke)
        val referenceExpression = context.attempt(ExpressionRule.defaultValue)
            ?: return ParseRule.Result.Failure.Rewind()

        val next = context.peek()

        if (next.type != TokenTypes.LParen) {
            return +ReferenceCallNode(referenceExpression.firstToken, referenceExpression.lastToken, emptyList(), referenceExpression)
        }

        val delimitedRule = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, ExpressionRule.defaultValue)
        val delimitedNode = context.attempt(delimitedRule)
            ?: TODO("@MethodCallRule:22")

        return +ReferenceCallNode(referenceExpression.firstToken, delimitedNode.lastToken, delimitedNode.nodes, referenceExpression)
    }
}

object MethodCallRule : CallRule<MethodCallNode> {
    private fun parseRightHandSide(context: Parser) : Pair<IdentifierNode, List<IExpressionNode>>? {
        val identifier = context.attempt(IdentifierRule)
            ?: return null

        val delim = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, ExpressionRule.defaultValue)
        val delimResult = context.attempt(delim)
            ?: return null

        return Pair(identifier, delimResult.nodes)
    }

    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val start = context.peek()
        var lhs = context.attemptAny(listOf(TypeExpressionRule, LiteralRule()))
            as? IExpressionNode
            ?: return ParseRule.Result.Failure.Rewind(context.end())

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(context.end())

        var next = context.peek()

        if (next.type != TokenTypes.Dot) {
            // TODO - I don't like this at all (abusing ParseRule to circumvent type checking on the returned Node type)
            // However, rewinding here causes an infinite loop due to an ambiguity in the grammar.
//            context.rewind(context.end())
//            val node = context.attemptAny(listOf(TypeExpressionRule, LiteralRule()))
//                ?: return ParseRule.Result.Failure.Abort
//
//            return +node
            return +lhs
        }

        context.expect(TokenTypes.Dot)

        next = context.peek()
        val message = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Throw("Expected method identifier after `.`, found `${next.text}`", next)

        next = context.peek()

        if (next.type != TokenTypes.LParen) {
            val end = context.consume()
            return +MethodCallNode(start, end, lhs, message, emptyList(), true)
        }

        val delim = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, ExpressionRule.defaultValue)
        val delimResult = context.attempt(delim)
            ?: TODO("HERE!!@£")

        if (!context.hasMore) return +MethodCallNode(start, delimResult.lastToken, lhs, message, delimResult.nodes, false)

        next = context.peek()

        if (next.type != TokenTypes.Dot) return +MethodCallNode(start, delimResult.lastToken, lhs, message, delimResult.nodes, false)

        // For every `.` we find, shift the current MethodCallNode left and parse a new `rhs`
        var rhs = Pair(message, delimResult.nodes)
        while (next.type == TokenTypes.Dot) {
            lhs = MethodCallNode(start, delimResult.lastToken, lhs, rhs.first, rhs.second, false)

            context.expect(TokenTypes.Dot)

            rhs = parseRightHandSide(context)
                ?: return ParseRule.Result.Failure.Abort

            if (!context.hasMore) break

            next = context.peek()
        }

        return +MethodCallNode(start, delimResult.lastToken, lhs, rhs.first, rhs.second, false)
    }
}
