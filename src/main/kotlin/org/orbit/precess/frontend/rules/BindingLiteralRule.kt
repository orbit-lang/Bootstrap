package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.BindingLiteralNode

object BindingLiteralRule : ParseRule<BindingLiteralNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val ref = context.attempt(RefLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.Bind)

        val type = context.attempt(TermExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        return +BindingLiteralNode(ref.firstToken, type.lastToken, ref, type)
    }
}