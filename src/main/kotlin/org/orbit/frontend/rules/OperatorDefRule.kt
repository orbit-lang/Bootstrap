package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.OperatorDefNode
import org.orbit.core.nodes.OperatorFixity
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object OperatorDefRule : ParseRule<OperatorDefNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expectAny(TokenTypes.Prefix, TokenTypes.Infix, TokenTypes.Postfix, consumes = true)
        val fixity = OperatorFixity.valueOf(start)
            ?: throw invocation.make<Parser>("Unknown Operator Fixity: ${start.text}", start)

        context.expect(TokenTypes.Operator)

        val identifier = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Abort

        val symbol = context.expect(TokenTypes.OperatorSymbol)

        val by = context.attempt(ByExpressionRule.methodReference)
            ?: return ParseRule.Result.Failure.Abort

        return +OperatorDefNode(start, by.lastToken, fixity, identifier, symbol.text.replace("`", ""), by.rhs)
    }
}