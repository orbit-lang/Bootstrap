package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

private object LambdaLiteralBodyRule : ParseRule<BlockNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val node = context.attemptAny(listOf(BlockRule.lambda, ExpressionRule.singleExpressionBodyRule))
            ?: return ParseRule.Result.Failure.Abort

        val body = when (node) {
            is BlockNode -> node
            is ExpressionNode -> node.toBlockNode()
            else -> TODO("@LambdaLiteralRule:16")
        }

        return +body
    }
}

private object ParameterlessLambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.peek()

        if (start.type != TokenTypes.LBrace) return ParseRule.Result.Failure.Abort

        val body = context.attempt(BlockRule.lambda)
            ?: return ParseRule.Result.Failure.Abort

        return +LambdaLiteralNode(start, body.lastToken, emptyList(), body)
    }
}

private object SingleParameterLambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val start = context.peek()

        val bindings = when (start.type) {
            TokenTypes.Identifier -> when (start.text) {
                "_" -> emptyList()
                else -> {
                    val name = context.attempt(IdentifierRule)!!
                    val type = TypeIdentifierNode.any()
                    val next = context.peek()

                    if (next.type != TokenTypes.In) return ParseRule.Result.Failure.Rewind(listOf(name.firstToken))

                    listOf(ParameterNode(start, start, name, type, null))
                }
            }

            else -> return ParseRule.Result.Failure.Abort
        }

        val recorded = context.end()

        if (context.peek().type != TokenTypes.In) return ParseRule.Result.Failure.Rewind(recorded)

        context.expect(TokenTypes.In)

        val body = context.attempt(LambdaLiteralBodyRule)
            ?: return ParseRule.Result.Failure.Abort

        return +LambdaLiteralNode(start, body.lastToken, bindings, body)
    }
}

private object UntypedParametersLambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val delim = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, IdentifierRule)
        val delimResult = context.attempt(delim)
        val recorded = context.end()

        if (delimResult == null) return ParseRule.Result.Failure.Rewind(recorded)
        if (context.peek().type != TokenTypes.In) return ParseRule.Result.Failure.Rewind(recorded)

        context.expect(TokenTypes.In)

        val bindings = delimResult.nodes.map { ParameterNode(it.firstToken, it.lastToken, it, TypeIdentifierNode.any(), null) }
        val body = context.attempt(LambdaLiteralBodyRule)
            ?: return ParseRule.Result.Failure.Abort

        return +LambdaLiteralNode(delimResult.firstToken, body.lastToken, bindings, body)
    }
}

private object TypedParametersLambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val delim = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, ParameterRule)
        val delimResult = context.attempt(delim)
        val recorded = context.end()

        if (delimResult == null) return ParseRule.Result.Failure.Rewind(recorded)
        if (context.peek().type != TokenTypes.In) return ParseRule.Result.Failure.Rewind(recorded)

        context.expect(TokenTypes.In)

        val bindings = delimResult.nodes
        val body = context.attempt(LambdaLiteralBodyRule)
            ?: return ParseRule.Result.Failure.Abort

        return +LambdaLiteralNode(delimResult.firstToken, body.lastToken, bindings, body)
    }
}

object LambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val node = context.attemptAny(listOf(ParameterlessLambdaLiteralRule, SingleParameterLambdaLiteralRule, TypedParametersLambdaLiteralRule, UntypedParametersLambdaLiteralRule))
            as? LambdaLiteralNode
            ?: return ParseRule.Result.Failure.Abort

        return +node
    }
}