package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.nodes.TermExpressionNode

object TermExpressionRule : ParseRule<TermExpressionNode<*>> {
    override fun parse(context: Parser): ParseRule.Result = when (val res = context.attemptAny(listOf(ArrowRule, TypeLookupRule))) {
        null -> ParseRule.Result.Failure.Abort
        else -> +res
    }
}