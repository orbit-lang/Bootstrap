package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.IWithStatementNode
import org.orbit.core.nodes.WithNode
import org.orbit.core.nodes.toWithNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

class WithRule(private val statementRules: List<ParseRule<out IWithStatementNode>>) : ParseRule<WithNode<IWithStatementNode>> {
    constructor(vararg statementRules: ParseRule<out IWithStatementNode>) : this(statementRules.toList())

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.With)
        val node = context.attemptAny(statementRules)
            as? IWithStatementNode
            ?: return ParseRule.Result.Failure.Abort

        return +node.toWithNode(start, node.lastToken)
    }
}