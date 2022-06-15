package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.IdentifierNode
import org.orbit.core.nodes.OperatorDefNode
import org.orbit.core.nodes.OperatorFixity
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object OperatorSymbolRule : ParseRule<IdentifierNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.OperatorSymbol)
        var next = context.peek()
        var symbol = ""
        while (next.type != TokenTypes.OperatorSymbol) {
            val op = context.expect(TokenTypes.OperatorSymbol)
            symbol += op.text
            next = context.peek()
        }

        val end = context.expect(TokenTypes.OperatorSymbol)

        return +IdentifierNode(start, end, symbol)
    }
}

object OperatorDefRule : ParseRule<OperatorDefNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Fixity)
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