package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ContextCompositionNode
import org.orbit.core.nodes.ContextExpressionNode
import org.orbit.core.nodes.ContextInstantiationNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

interface ContextCompositionOperator {
    val symbol: String
}

enum class IntrinsicContextCompositionOperator(override val symbol: String) : ContextCompositionOperator {
    And("&"), Or("|");

    companion object {
        fun valueOfOrNull(symbol: String) : IntrinsicContextCompositionOperator? = when (val idx = allSymbols().indexOf(symbol)) {
            -1 -> null
            else -> values()[idx]
        }

        fun allSymbols() : List<String>
            = values().map { it.symbol }
    }
}

data class UserDefinedContextCompositionOperator(override val symbol: String) : ContextCompositionOperator

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

        if (next.type == TokenTypes.Operator) {
            val op = when (val op = IntrinsicContextCompositionOperator.valueOfOrNull(next.text)) {
                null -> UserDefinedContextCompositionOperator(next.text)
                else -> op
            }

            context.consume()

            val leftContext = ContextInstantiationNode(contextIdentifier.firstToken, delim.lastToken, contextIdentifier, delim.nodes)
            val rightContext = context.attemptAny(ContextExpressionRule.any)
                as? ContextExpressionNode
                ?: TODO("Context Composition Rule - Missing Right-hand Side")

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