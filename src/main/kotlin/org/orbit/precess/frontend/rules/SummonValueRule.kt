package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.SummonValueNode

object SummonValueRule : ParseRule<SummonValueNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.SummonValue)
        val term = context.attempt(AnyTermExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.As)

        val ref = context.attempt(RefLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        return +SummonValueNode(start, ref.lastToken, term, ref)
    }
}