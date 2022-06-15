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

//        context.expect(TokenTypes.Dot)

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

//class PartialCallRule(private val receiverExpression: ExpressionNode) : ValueRule<MethodCallNode> {
//    override fun parse(context: Parser): ParseRule.Result {
//        context.expectOrNull(TokenTypes.Dot)
//            ?: return ParseRule.Result.Failure.Rewind(listOf(receiverExpression.firstToken))
//
//        val methodIdentifier = context.attempt(IdentifierRule)
//            ?: throw context.invocation.make<Parser>("Expected method name", context.peek())
//
//        var next = context.peek()
//
//        if (next.type != TokenTypes.LParen) {
//            // This looks like a parameter-less call, which is essentially property access
//            return +MethodCallNode(receiverExpression.firstToken, methodIdentifier.lastToken, receiverExpression, methodIdentifier, emptyList(), true)
//        }
//
//        // We're now parsing a method call
//        // Eat the opening '('
//        context.consume()
//
//        next = context.peek()
//
//        if (next.type == TokenTypes.RParen) {
//            context.consume()
//            val rec = when (receiverExpression) {
//                is RValueNode -> receiverExpression.expressionNode
//                else -> receiverExpression
//            }
//
//            return +MethodCallNode(receiverExpression.firstToken, methodIdentifier.lastToken, receiverExpression, methodIdentifier, when (rec) {
//                is TypeExpressionNode -> emptyList<ExpressionNode>()
//                else -> listOf(receiverExpression)
//            })
////            return parseTrailing(context, MethodCallNode(receiverExpression.firstToken, methodIdentifier.lastToken, receiverExpression, methodIdentifier, when (rec) {
////                is TypeExpressionNode -> emptyList<ExpressionNode>()
////                else -> listOf(receiverExpression)
////            }))
//        }
//
//        val parameterNodes = mutableListOf<ExpressionNode>()
//        while (next.type != TokenTypes.RParen) {
//            val expression = context.attempt(ExpressionRule.defaultValue)
//                ?: throw context.invocation.make<Parser>("Method call parameters must be expressions", next.position)
//
//            parameterNodes.add(expression)
//
//            next = context.peek()
//
//            if (next.type == TokenTypes.Comma) {
//                context.consume()
//                next = context.peek()
//            }
//        }
//
//        val last = context.expect(TokenTypes.RParen)
//
//        return +MethodCallNode(receiverExpression.firstToken, last, receiverExpression, methodIdentifier, parameterNodes)
////        return parseTrailing(context, MethodCallNode(receiverExpression.firstToken, last, receiverExpression, methodIdentifier, parameterNodes))
//    }
//}
