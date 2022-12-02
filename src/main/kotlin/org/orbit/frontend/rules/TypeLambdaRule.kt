package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.TypeLambdaNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TypeLambdaRule : ParseRule<TypeLambdaNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val delimRule = DelimitedRule(innerRule = TypeExpressionRule)
        val delim = context.attempt(delimRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        context.expect(TokenTypes.Assignment)
        context.expect(TokenTypes.RAngle)

        val domain = delim.nodes
        val codomain = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Type Expression on right-hand side of Type Lambda", collector.getCollectedTokens().last())

        return +TypeLambdaNode(delim.firstToken, codomain.lastToken, domain, codomain)
    }
}