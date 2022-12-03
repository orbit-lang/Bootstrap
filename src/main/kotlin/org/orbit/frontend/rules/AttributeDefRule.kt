package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AttributeOperatorExpressionRule : ParseRule<AttributeOperatorExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val lExpr = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        val op = context.expectAny(TokenTypes.OperatorSymbol, TokenTypes.Assignment, TokenTypes.Colon, TokenTypes.Identifier, consumes = true)
        val boundsType = TypeBoundsOperator.valueOf(op)
        val rExpr = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Type Expression on right-hand side of Attribute Operator Expression", collector.getCollectedTokens().last())

        return +AttributeOperatorExpressionNode(lExpr.firstToken, rExpr.lastToken, boundsType, lExpr, rExpr)
    }
}

object AttributeInvocationRule : ParseRule<AttributeInvocationNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val expr = context.attempt(TypeLambdaInvocationRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        return +AttributeInvocationNode(expr.firstToken, expr.lastToken, expr.typeIdentifierNode, expr.arguments)
    }
}

object AnyAttributeExpressionRule : ParseRule<IAttributeExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val node = context.attemptAny(listOf(AttributeOperatorExpressionRule, AttributeInvocationRule))
            as? IAttributeExpressionNode
            ?: return ParseRule.Result.Failure.Rewind(collector)

        return +node
    }
}

object AttributeArrowRule : ParseRule<AttributeArrowNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val delimRule = DelimitedRule(innerRule = TypeIdentifierRule.Naked)
        val delim = context.attempt(delimRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        if (!context.hasAtLeast(2)) return ParseRule.Result.Failure.Abort

        val next = context.peek()

        if (next.type != TokenTypes.Assignment) {
            return ParseRule.Result.Failure.Rewind(collector)
        }

        context.expect(TokenTypes.Assignment)
        context.expect(TokenTypes.RAngle)

        val parameters = delim.nodes
        val constraint = context.attempt(AnyAttributeExpressionRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Attribute expression on right-hand side of Attribute Arrow", delim.lastToken)

        return +AttributeArrowNode(delim.firstToken, constraint.lastToken, parameters, constraint)
    }
}

object AttributeDefRule : ParseRule<AttributeDefNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Attribute)
        val identifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Throw("Expected Type Identifier after `attribute`", start)

        context.expect(TokenTypes.Assignment)

        val next = context.peek()
        val expr = context.attempt(AttributeArrowRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Attribute Arrow on right-hand side of Attribute declaration `attribute ${identifier.value}`", next)

        return +AttributeDefNode(start, expr.lastToken, identifier, expr)
    }
}