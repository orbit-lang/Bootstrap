package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.ParseRule
import org.orbit.frontend.Parser
import org.orbit.frontend.TokenTypes
import org.orbit.frontend.unaryPlus

fun <N: Node> ParseRule<N>.parseTrailing(context: Parser, result: ExpressionNode) : ParseRule.Result {
    val next = context.peek()

    if (next.type == TokenTypes.Dot) {
        val partialCallRule = PartialCallRule(result)

        return partialCallRule.execute(context)
    } else if (next.type == TokenTypes.Operator) {
        val partialExpressionRule = PartialExpressionRule(result)

        return partialExpressionRule.execute(context)
    }

    return +result
}

object CallRule : ValueRule<CallNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val receiverExpression = context.attempt(ExpressionRule.defaultValue)
            ?: throw context.invocation.make<Parser>("TODO", context.peek())

        context.expectOrNull(TokenTypes.Dot)
            ?: return ParseRule.Result.Failure.Rewind(listOf(receiverExpression.firstToken))

        val methodIdentifier = context.attempt(IdentifierRule)
            ?: throw context.invocation.make<Parser>("Expected method name", context.peek())

        var next = context.peek()

        if (next.type != TokenTypes.LParen) {
            // This looks like a parameter-less call, which is property access
            return parseTrailing(context, CallNode(receiverExpression.firstToken, methodIdentifier.lastToken, receiverExpression, methodIdentifier, emptyList(), true))
        }

        // We're now parsing a method call
        // Eat the opening '('
        context.consume()

        next = context.peek()

        if (next.type == TokenTypes.RParen) {
            context.consume()
            return +CallNode(receiverExpression.firstToken, methodIdentifier.lastToken, receiverExpression, methodIdentifier, emptyList())
        }

        val parameterNodes = mutableListOf<ExpressionNode>()
        while (next.type != TokenTypes.RParen) {
            val expression = context.attempt(ExpressionRule.defaultValue)
                ?: throw context.invocation.make<Parser>("Method call parameters must be expressions", next.position)

            parameterNodes.add(expression)

            next = context.peek()

            if (next.type == TokenTypes.Comma) {
                context.consume()
                next = context.peek()
            }
        }

        val last = context.expect(TokenTypes.RParen)

        return parseTrailing(context, CallNode(receiverExpression.firstToken, last, receiverExpression, methodIdentifier, parameterNodes))
    }
}

class PartialCallRule(private val receiverExpression: ExpressionNode) : ValueRule<CallNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.expectOrNull(TokenTypes.Dot)
            ?: return ParseRule.Result.Failure.Rewind(listOf(receiverExpression.firstToken))

        val methodIdentifier = context.attempt(IdentifierRule)
            ?: throw context.invocation.make<Parser>("Expected method name", context.peek())

        var next = context.peek()

        if (next.type != TokenTypes.LParen) {
            // This looks like a parameter-less call, which is essentially property access
            return +CallNode(receiverExpression.firstToken, methodIdentifier.lastToken, receiverExpression, methodIdentifier, emptyList(), true)
        }

        // We're now parsing a method call
        // Eat the opening '('
        context.consume()

        next = context.peek()

        if (next.type == TokenTypes.RParen) {
            context.consume()
            return parseTrailing(context, CallNode(receiverExpression.firstToken, methodIdentifier.lastToken, receiverExpression, methodIdentifier, emptyList()))
        }

        val parameterNodes = mutableListOf<ExpressionNode>()
        while (next.type != TokenTypes.RParen) {
            val expression = context.attempt(ExpressionRule.defaultValue)
                ?: throw context.invocation.make<Parser>("Method call parameters must be expressions", next.position)

            parameterNodes.add(expression)

            next = context.peek()

            if (next.type == TokenTypes.Comma) {
                context.consume()
                next = context.peek()
            }
        }

        val last = context.expect(TokenTypes.RParen)

        return parseTrailing(context, CallNode(receiverExpression.firstToken, last, receiverExpression, methodIdentifier, parameterNodes))
    }
}

//object TypeMethodCallRule : MethodCallRule<TypeMethodCallNode> {
//    override fun parse(context: Parser): ParseRule.Result {
//        val typeIdentifier = context.attempt(TypeIdentifierRule.RValue)
//            ?: throw context.invocation.make<Parser>("", context.peek())
//
//        context.expectOrNull(TokenTypes.Dot)
//            ?: return ParseRule.Result.Failure.Rewind(listOf(typeIdentifier.firstToken))
//
//        val methodIdentifier = context.attempt(IdentifierRule)
//            ?: throw context.invocation.make<Parser>("Expected method name", context.peek())
//
//        context.expect(TokenTypes.LParen)
//
//        var next = context.peek()
//
//        if (next.type == TokenTypes.RParen) {
//            context.consume()
//            return +TypeMethodCallNode(typeIdentifier.firstToken, next, methodIdentifier, emptyList(), typeIdentifier)
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
//        return +TypeMethodCallNode(typeIdentifier.firstToken, last, methodIdentifier, parameterNodes, typeIdentifier)
//    }
//}

//object InstanceMethodCallRule : MethodCallRule<InstanceMethodCallNode> {
//    override fun parse(context: Parser): ParseRule.Result {
//        val instanceIdentifier = context.attempt(IdentifierRule)
//            ?: throw context.invocation.make<Parser>("Expected identifier", context.peek())
//
//        context.expectOrNull(TokenTypes.Dot)
//            ?: return ParseRule.Result.Failure.Rewind(listOf(instanceIdentifier.firstToken))
//
//        val methodIdentifier = context.attempt(IdentifierRule)
//            ?: throw context.invocation.make<Parser>("Expected method name", context.peek())
//
//        context.expect(TokenTypes.LParen)
//
//        var next = context.peek()
//
//        if (next.type == TokenTypes.RParen) {
//            context.consume()
//            return +InstanceMethodCallNode(instanceIdentifier.firstToken, next, methodIdentifier, instanceIdentifier, emptyList())
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
//        return +InstanceMethodCallNode(instanceIdentifier.firstToken, last, methodIdentifier, instanceIdentifier, parameterNodes)
//    }
//}