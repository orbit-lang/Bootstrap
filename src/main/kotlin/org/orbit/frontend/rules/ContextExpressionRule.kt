package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ContextExpressionNode
import org.orbit.core.nodes.ContextInstantiationNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object ContextInstantiationRule : ParseRule<ContextExpressionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val contextIdentifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Abort

        val typeVariablesRule = DelimitedRule(TokenTypes.LBracket, TokenTypes.RBracket, TypeExpressionRule)
        val next = context.peek()
        val delim = context.attempt(typeVariablesRule)
            ?: throw invocation.make<Parser>("Expected concrete Type Variables for Context instantiation", next)

        return +ContextInstantiationNode(contextIdentifier.firstToken, delim.lastToken, contextIdentifier, delim.nodes)
    }
}

object ContextExpressionRule : ParseRule<ContextExpressionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        context.expect(TokenTypes.Within)

        val next = context.peek()
        val expr = context.attempt(ContextInstantiationRule)
            ?: throw invocation.make<Parser>("Expected Context expression after `within`", next)

        return +expr
    }
}