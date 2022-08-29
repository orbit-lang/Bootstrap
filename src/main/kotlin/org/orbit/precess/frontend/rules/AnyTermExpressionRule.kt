package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.nodes.TermExpressionNode

object AnyTermExpressionRule : ParseRule<TermExpressionNode<*>> {
    override fun parse(context: Parser): ParseRule.Result {
        val node = context.attemptAny(listOf(RefExprRule, TypeLookupRule, ArrowRule, SafeRule, BoxRule))
            as? TermExpressionNode<*>
            ?: return ParseRule.Result.Failure.Abort

        return +node
    }
}