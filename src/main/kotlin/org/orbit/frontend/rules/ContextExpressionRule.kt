package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.IIntrinsicOperator
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ContextCompositionNode
import org.orbit.core.nodes.ContextExpressionNode
import org.orbit.core.nodes.ContextInstantiationNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.backend.typesystem.components.parse
import org.orbit.util.Invocation

enum class IntrinsicContextCompositionOperator(override val symbol: String) : IIntrinsicOperator {
    And("&"), Or("|");

    companion object : IIntrinsicOperator.Factory<IntrinsicContextCompositionOperator> {
        override fun all(): List<IntrinsicContextCompositionOperator>
            = values().toList()
    }
}

object ContextInstantiationRule : ParseRule<ContextExpressionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val contextIdentifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Abort

        val typeVariablesRule = DelimitedRule(TokenTypes.LBracket, TokenTypes.RBracket, TypeExpressionRule)
        var next = context.peek()
        val delim = context.attempt(typeVariablesRule)
            ?: throw invocation.make<Parser>("Expected concrete Type Variables for Context instantiation", next)

        next = context.peek()

        if (next.type == TokenTypes.OperatorSymbol) {
            val op = IntrinsicContextCompositionOperator.parse(next.text)
                ?: throw invocation.make<Parser>("Unknown Context Operator ${next.text}", next)

            context.consume()

            val leftContext = ContextInstantiationNode(contextIdentifier.firstToken, delim.lastToken, contextIdentifier, delim.nodes)
            val rightContext = context.attemptAny(ContextExpressionRule.any)
                as? ContextExpressionNode
                ?: throw invocation.make<Parser>("Expected expression on right-hand side of Context Instantiation", leftContext.lastToken)

            return +ContextCompositionNode(contextIdentifier.firstToken, rightContext.lastToken, op, leftContext, rightContext)
        }

        return +ContextInstantiationNode(contextIdentifier.firstToken, delim.lastToken, contextIdentifier, delim.nodes)
    }
}

object ContextExpressionRule : ParseRule<ContextExpressionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    val any = listOf(ContextInstantiationRule)

    override fun parse(context: Parser): ParseRule.Result {
        context.expect(TokenTypes.Within)

        val next = context.peek()
        val expr = context.attempt(ContextInstantiationRule)
            ?: throw invocation.make<Parser>("Expected Context expression after `within`", next)

        return +expr
    }
}