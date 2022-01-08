package org.orbit.frontend.rules

import org.orbit.core.nodes.BlockNode
import org.orbit.core.nodes.DelimitedNode
import org.orbit.core.nodes.LambdaLiteralNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object LambdaLiteralRule : ValueRule<LambdaLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val delimitedRule = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, PairRule)
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

        return +LambdaLiteralNode(delimitedNode.firstToken, body.lastToken, delimitedNode.nodes, body)
    }
}