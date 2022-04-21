package org.orbit.frontend.rules

import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.nodes.BlockNode
import org.orbit.core.nodes.LambdaLiteralNode
import org.orbit.core.nodes.PairNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object LambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    private val nullToken = Token(TokenTypes.TypeIdentifier, "AnyType", SourcePosition.unknown)
    private val anyTypeIdentifierNode = TypeIdentifierNode(nullToken, nullToken, "AnyType")

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.peek()

        if (start.type == TokenTypes.LBrace) {
            // This lambda has no parameters
            val body = context.attempt(BlockRule.lambda)
                ?: TODO("@LambdaLiteralRule:23")

            return +LambdaLiteralNode(body.firstToken, body.lastToken, emptyList(), body)
        }

        val delimitedRule = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, EitherRule(PairRule, IdentifierRule))
        val delimitedNode = context.attempt(delimitedRule)
            ?: return ParseRule.Result.Failure.Rewind()

        context.expect(TokenTypes.In)

        val next = context.peek()
        var body = context.attempt(when (next.type) {
            TokenTypes.LBrace -> BlockRule.lambda
            else -> ExpressionRule.defaultValue
        }) ?: TODO("@LambdaLiteralRule:17")

        body = when (body) {
            is BlockNode -> body
            else -> BlockNode(body.firstToken, body.lastToken, listOf(body))
        }

        val bindings = delimitedNode.nodes.map { when (it.isRight) {
            true -> PairNode(it.rightNode!!.firstToken, it.rightNode!!.lastToken, it.rightNode!!, anyTypeIdentifierNode)
            else -> it.leftNode!!
        }}

        return +LambdaLiteralNode(delimitedNode.firstToken, body.lastToken, bindings, body)
    }
}