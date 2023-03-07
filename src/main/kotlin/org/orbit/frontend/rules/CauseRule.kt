package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.CauseNode
import org.orbit.core.nodes.EffectInvocationNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object CauseRule : ValueRule<CauseNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Cause)

        context.expect(TokenTypes.Dot)

        val next = context.peek()
        val effectIdentifier = context.attempt(TypeIdentifierRule.Naked)
            ?: throw invocation.make<Parser>("Expected Effect identified after `cause .`", next)

        val delim = DelimitedRule(innerRule = ExpressionRule.defaultValue)
        val delimNode = context.attempt(delim)
            ?: return ParseRule.Result.Failure.Abort

        val effect = EffectInvocationNode(effectIdentifier.firstToken, delimNode.lastToken, effectIdentifier, delimNode.nodes)

        return +CauseNode(start, delimNode.lastToken, effect)
    }
}