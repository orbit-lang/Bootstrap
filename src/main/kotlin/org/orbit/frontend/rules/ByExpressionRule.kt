package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ByExpressionNode
import org.orbit.core.nodes.INode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

data class ByExpressionRule<N: INode>(private val rhsRule: ParseRule<N>) : ParseRule<ByExpressionNode<N>> {
    companion object {
        val methodReference = ByExpressionRule(InvokableReferenceRule)
    }

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.By)
        val rhs = context.attempt(rhsRule)
            ?: return ParseRule.Result.Failure.Abort

        return +ByExpressionNode(start, rhs.lastToken, rhs)
    }
}