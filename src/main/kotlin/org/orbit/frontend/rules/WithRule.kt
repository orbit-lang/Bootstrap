package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.IWithStatementNode
import org.orbit.core.nodes.WithNode
import org.orbit.core.nodes.toWithNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

class WithRule<N: IWithStatementNode>(private val statementRule: ParseRule<N>) : ParseRule<WithNode<N>> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.With)
        val node = context.attempt(statementRule)
            ?: return ParseRule.Result.Failure.Throw("Expected With statement after `with...`", start)

        return +node.toWithNode(start, node.lastToken)
    }
}