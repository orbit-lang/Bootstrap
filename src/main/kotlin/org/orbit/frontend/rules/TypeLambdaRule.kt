package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.typesystem.phase.TypeSystem
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.AttributeOperatorExpressionNode
import org.orbit.core.nodes.TypeLambdaConstraintNode
import org.orbit.core.nodes.TypeLambdaNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object TypeLambdaConstraintRule : ParseRule<TypeLambdaConstraintNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Where)
        val next = context.peek()
        val expr = context.attempt(AnyAttributeExpressionRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Attribute invocation expression after `where`\n\te.g. `where .Equal(A, B)`", next)

        if (expr is AttributeOperatorExpressionNode) {
            return ParseRule.Result.Failure.Throw("Attribute Operators as Type Lambda Constraints are unsupported", next)
        }

        return +TypeLambdaConstraintNode(start, expr.lastToken, expr)
    }
}

object TypeLambdaRule : ParseRule<TypeLambdaNode>, KoinComponent {
    private val invocation: Invocation by inject()

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

        if (next.type == TokenTypes.Else) {
            if (constraints.isEmpty()) {
                throw invocation.make<TypeSystem>("Found `else` clause but no constraints declared in Type Lambda", next)
            }

            context.consume()

            val elseClause = context.attempt(TypeExpressionRule)
                ?: throw invocation.make<TypeSystem>("Expected Type Expression after `else` clause in Type Lambda", next)

            return +TypeLambdaNode(delim.firstToken, codomain.lastToken, domain, codomain, constraints, elseClause)
        }

        return +TypeLambdaNode(delim.firstToken, codomain.lastToken, domain, codomain, constraints)
    }
}