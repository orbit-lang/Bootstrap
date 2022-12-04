package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.TypeLambdaConstraintNode
import org.orbit.core.nodes.TypeLambdaNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TypeLambdaConstraintRule : ParseRule<TypeLambdaConstraintNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Where)
        val next = context.peek()
        val invocation = context.attempt(AttributeInvocationRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Attribute invocation expression after `where`\n\te.g. `where .Equal(A, B)`", next)

        return +TypeLambdaConstraintNode(start, invocation.lastToken, invocation)
    }
}

object TypeLambdaRule : ParseRule<TypeLambdaNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val delimRule = DelimitedRule(innerRule = TypeExpressionRule)
        val delim = context.attempt(delimRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        if (!context.hasAtLeast(2)) return ParseRule.Result.Failure.Abort

        var next = context.peek()

        if (next.type != TokenTypes.Assignment) {
            return ParseRule.Result.Failure.Rewind(collector)
        }

        context.expect(TokenTypes.Assignment)
        context.expect(TokenTypes.RAngle)

        val domain = delim.nodes
        val codomain = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Type Expression on right-hand side of Type Lambda", collector.getCollectedTokens().last())

        if (!context.hasMore) return +TypeLambdaNode(delim.firstToken, codomain.lastToken, domain, codomain, emptyList())

        next = context.peek()

        val constraints = mutableListOf<TypeLambdaConstraintNode>()
        while (next.type == TokenTypes.Where) {
            val constraint = context.attempt(TypeLambdaConstraintRule)
                ?: return ParseRule.Result.Failure.Abort

            constraints.add(constraint)

            next = context.peek()
        }

        return +TypeLambdaNode(delim.firstToken, codomain.lastToken, domain, codomain, constraints)
    }
}