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
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Call)
        val receiver = context.attemptAny(listOf(TypeExpressionRule, ExpressionRule.defaultValue))
            as? ExpressionNode
            ?: throw context.invocation.make<Parser>("TODO", context.peek())

        val message = context.attempt(IdentifierRule)
            ?: TODO("HJEKHJASD F")

        val next = context.peek()

        if (next.type != TokenTypes.LParen) {
            val end = context.consume()
            return +MethodCallNode(start, end, receiver, message, emptyList(), true)
        }

        val delim = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, ExpressionRule.defaultValue)
        val delimResult = context.attempt(delim)
            ?: TODO("HERE!!@£")

        return +MethodCallNode(start, delimResult.lastToken, receiver, message, delimResult.nodes, false)
    }
}
