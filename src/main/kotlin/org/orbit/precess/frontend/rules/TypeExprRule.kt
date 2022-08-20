package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.nodes.TypeExprNode

object TypeExprRule : ParseRule<TypeExprNode> {
    override fun parse(context: Parser): ParseRule.Result = when (val res = context.attemptAny(listOf(ArrowRule, TypeLiteralRule))) {
        null -> ParseRule.Result.Failure.Abort
        else -> +res
    }
}