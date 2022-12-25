package org.orbit.frontend.rules

import org.orbit.core.nodes.IAttributeExpressionNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AnyAttributeExpressionRule : ParseRule<IAttributeExpressionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val node = context.attemptAny(listOf(AttributeOperatorExpressionRule, AttributeInvocationRule))
            as? IAttributeExpressionNode
            ?: return ParseRule.Result.Failure.Rewind(collector)

        return +node
    }
}