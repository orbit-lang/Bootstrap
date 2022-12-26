package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.AttributeArrowNode
import org.orbit.core.nodes.TypeEffectInvocationNode
import org.orbit.core.nodes.TypeLambdaInvocationNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TypeEffectInvocationRule : ParseRule<TypeEffectInvocationNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Dot)
        val identifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Abort

        val delimRule = DelimitedRule(innerRule = TypeIdentifierRule.Naked)
        val delim = context.attempt(delimRule)
            ?: return ParseRule.Result.Failure.Abort

        return +TypeEffectInvocationNode(start, delim.lastToken, identifier, delim.nodes)
    }
}

object AttributeArrowRule : ParseRule<AttributeArrowNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val delimRule = DelimitedRule(innerRule = TypeIdentifierRule.Naked)
        val delim = context.attempt(delimRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        if (!context.hasAtLeast(2)) return ParseRule.Result.Failure.Abort

        var next = context.peek()

        if (next.type != TokenTypes.Assignment) {
            return ParseRule.Result.Failure.Rewind(collector)
        }

        context.expect(TokenTypes.Assignment)
        context.expect(TokenTypes.RAngle)

        val parameters = delim.nodes
        val constraint = context.attempt(AnyAttributeExpressionRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Attribute expression on right-hand side of Attribute Arrow", delim.lastToken)

        next = context.peek()

        val effects = mutableListOf<TypeEffectInvocationNode>()
        while (next.type == TokenTypes.With) {
            context.consume()

            val effect = context.attempt(TypeEffectInvocationRule)
                ?: return ParseRule.Result.Failure.Throw("Expected Effect invocation after `with` in Attribute definition", next)

            effects.add(effect)

            next = context.peek()
        }

        return +AttributeArrowNode(delim.firstToken, constraint.lastToken, parameters, constraint, effects)
    }
}