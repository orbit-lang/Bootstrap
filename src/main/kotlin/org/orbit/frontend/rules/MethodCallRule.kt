package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.parseTrailing
import org.orbit.frontend.extensions.unaryPlus

interface CallRule<N: InvokableNode> : ValueRule<N>

object ReferenceCallRule : CallRule<ReferenceCallNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.expect(TokenTypes.Invoke)
        val referenceExpression = context.attempt(ExpressionRule.defaultValue)
            ?: return ParseRule.Result.Failure.Rewind()

        val next = context.peek()

        if (next.type != TokenTypes.LParen) return ParseRule.Result.Failure.Rewind()

        val delimitedRule = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, ExpressionRule.defaultValue)
        val delimitedNode = context.attempt(delimitedRule)
            ?: TODO("@MethodCallRule:22")

        return +ReferenceCallNode(referenceExpression.firstToken, delimitedNode.lastToken, delimitedNode.nodes, referenceExpression)
    }
}

object MethodCallRule : CallRule<MethodCallNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val receiverExpression = context.attempt(ExpressionRule.defaultValue)
            ?: throw context.invocation.make<Parser>("TODO", context.peek())

        var next = context.peek()

        if (receiverExpression is MethodCallNode && next.type != TokenTypes.Dot) {
            return +receiverExpression
        }

        context.expectOrNull(TokenTypes.Dot)
            ?: return ParseRule.Result.Failure.Rewind(listOf(receiverExpression.firstToken))

        val methodIdentifier = context.attempt(IdentifierRule)
            ?: throw context.invocation.make<Parser>("Expected method name", context.peek())

        next = context.peek()

        if (next.type != TokenTypes.LParen) {
            // This looks like a parameter-less call, which is property access
            return parseTrailing(context, MethodCallNode(receiverExpression.firstToken, methodIdentifier.lastToken, receiverExpression, methodIdentifier, emptyList(), true))
        }

        // We're now parsing a method call
        // Eat the opening '('
        context.consume()

        next = context.peek()

        if (next.type == TokenTypes.RParen) {
            context.consume()
            return +MethodCallNode(receiverExpression.firstToken, methodIdentifier.lastToken, receiverExpression, methodIdentifier, emptyList())
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

        return parseTrailing(context, MethodCallNode(receiverExpression.firstToken, last, receiverExpression, methodIdentifier, parameterNodes))
    }
}

class PartialCallRule(private val receiverExpression: ExpressionNode) : ValueRule<MethodCallNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.expectOrNull(TokenTypes.Dot)
            ?: return ParseRule.Result.Failure.Rewind(listOf(receiverExpression.firstToken))

        val methodIdentifier = context.attempt(IdentifierRule)
            ?: throw context.invocation.make<Parser>("Expected method name", context.peek())

        var next = context.peek()

        if (next.type != TokenTypes.LParen) {
            // This looks like a parameter-less call, which is essentially property access
            return +MethodCallNode(receiverExpression.firstToken, methodIdentifier.lastToken, receiverExpression, methodIdentifier, emptyList(), true)
        }

        // We're now parsing a method call
        // Eat the opening '('
        context.consume()

        next = context.peek()

        if (next.type == TokenTypes.RParen) {
            context.consume()
            return parseTrailing(context, MethodCallNode(receiverExpression.firstToken, methodIdentifier.lastToken, receiverExpression, methodIdentifier, emptyList()))
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

        return parseTrailing(context, MethodCallNode(receiverExpression.firstToken, last, receiverExpression, methodIdentifier, parameterNodes))
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