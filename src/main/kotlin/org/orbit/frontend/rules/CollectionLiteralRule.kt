package org.orbit.frontend.rules

import org.orbit.core.nodes.CollectionLiteralNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object CollectionLiteralRule : ValueRule<CollectionLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val delimitedRule = DelimitedRule(TokenTypes.LBracket, TokenTypes.RBracket, ExpressionRule.defaultValue)
        val members = context.attempt(delimitedRule)
            ?: return ParseRule.Result.Failure.Rewind(collector.getCollectedTokens())

        return +CollectionLiteralNode(members.firstToken, members.lastToken, members.nodes)
    }
}