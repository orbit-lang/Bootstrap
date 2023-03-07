package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.EffectNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object EffectRule : ParseRule<EffectNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val start = context.expect(TokenTypes.Effect)
        val identifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Throw("Expected Effect identifier after `effect`", collector)

        context.expect(TokenTypes.Assignment)

        val lambda = context.attempt(LambdaTypeRule)
            ?: return ParseRule.Result.Failure.Abort

        return +EffectNode(start, lambda.lastToken, identifier, lambda)
    }
}