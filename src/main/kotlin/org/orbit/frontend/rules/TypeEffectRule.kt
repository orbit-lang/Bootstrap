package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ITypeEffectExpressionNode
import org.orbit.core.nodes.ProjectionEffectNode
import org.orbit.core.nodes.TypeEffectNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ProjectionEffectRule : ParseRule<ProjectionEffectNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Projection)
        val type = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.Colon)

        val trait = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Abort

        return +ProjectionEffectNode(start, trait.lastToken, type, trait)
    }
}

object AnyTypeEffectExpressionRule : ParseRule<ITypeEffectExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result = when (val result = context.attempt(ProjectionEffectRule)) {
        null -> ParseRule.Result.Failure.Abort
        else -> +result
    }
}

object TypeEffectRule : ParseRule<TypeEffectNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Effect)
        val identifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Abort // TODO - Error message

        context.expect(TokenTypes.Assignment)

        val delimRule = DelimitedRule(innerRule = TypeIdentifierRule.Naked)
        val delim = context.attempt(delimRule)
            ?: return ParseRule.Result.Failure.Abort // TODO - Error message

        context.expect(TokenTypes.Assignment)
        context.expect(TokenTypes.RAngle)

        val body = context.attempt(AnyTypeEffectExpressionRule)
            ?: return ParseRule.Result.Failure.Abort // TODO - Error message

        return +TypeEffectNode(start, body.lastToken, identifier, delim.nodes, body)
    }
}