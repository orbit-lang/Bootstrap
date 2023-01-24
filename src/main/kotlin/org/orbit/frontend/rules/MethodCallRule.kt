package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
//import org.orbit.frontend.extensions.parseTrailing
import org.orbit.frontend.extensions.unaryPlus

interface CallRule<N: IInvokableNode> : ValueRule<N>

object InvocationRule : CallRule<InvocationNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val lhs = context.attempt(ExpressionRule.invocationRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(collector)

        val next = context.peek()

        if (next.type != TokenTypes.LParen) return ParseRule.Result.Failure.Rewind(collector)

        val delimRule = DelimitedRule(innerRule = ExpressionRule.defaultValue)
        val delim = context.attempt(delimRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        return +InvocationNode(lhs.firstToken, delim.lastToken, lhs, delim.nodes)
    }
}

object EffectHandlerRule : ParseRule<EffectHandlerNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val start = context.expect(TokenTypes.By)

        context.expect(TokenTypes.LBrace)

        val flowId = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Flow identifier in Effect Handler", collector)

        context.expect { it.text == "->" }

        var next = context.peek()
        val cases = mutableListOf<CaseNode>()
        while (next.type == TokenTypes.Case) {
            val case = context.attempt(CaseRule)
                ?: return ParseRule.Result.Failure.Abort

            cases.add(case)

            next = context.peek()
        }

        val end = context.expect(TokenTypes.RBrace)

        return +EffectHandlerNode(start, end, flowId, cases)
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

    private fun parseEffectHandler(context: Parser, partialResult: ParseRule.Result.Success<MethodCallNode>) : ParseRule.Result {
        val next = context.peek()
        if (next.type == TokenTypes.By) {
            // Parse Effect Handler
            val effectHandler = context.attempt(EffectHandlerRule)
                ?: return ParseRule.Result.Failure.Abort

            return +(partialResult.node.withEffectHandler(effectHandler))
        }

        return partialResult
    }

    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val start = context.peek()
        var lhs = context.attemptAny(listOf(TypeExpressionRule, LiteralRule()))
            as? IExpressionNode
            ?: return ParseRule.Result.Failure.Rewind(collector.getCollectedTokens())

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(collector.getCollectedTokens())

        var next = context.peek()

        if (next.type != TokenTypes.Dot) {
            if (lhs is TypeExpressionNode) {
                return ParseRule.Result.Failure.Rewind(collector.getCollectedTokens())
            }

            return +lhs
        }

        context.expect(TokenTypes.Dot)

        next = context.peek()
        val message = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Throw("Expected method identifier after `.`, found `${next.text}`", next)

        next = context.peek()

        if (next.type != TokenTypes.LParen) {
            return parseEffectHandler(context, +MethodCallNode(start, next, lhs, message, emptyList(), true))
        }

        val delim = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, ExpressionRule.defaultValue)
        val delimResult = context.attempt(delim)
            ?: TODO("HERE!!@£")

        if (!context.hasMore) return parseEffectHandler(context, +MethodCallNode(start, delimResult.lastToken, lhs, message, delimResult.nodes, false))

        next = context.peek()

        if (next.type != TokenTypes.Dot) return parseEffectHandler(context, +MethodCallNode(start, delimResult.lastToken, lhs, message, delimResult.nodes, false))

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

        return parseEffectHandler(context, +MethodCallNode(start, delimResult.lastToken, lhs, rhs.first, rhs.second, false))
    }
}
