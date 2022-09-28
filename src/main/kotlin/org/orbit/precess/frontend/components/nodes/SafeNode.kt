package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.backend.typesystem.components.Env
import org.orbit.precess.backend.components.Expr

data class SafeNode(override val firstToken: Token, override val lastToken: Token, val term: TermExpressionNode<*>) : TermExpressionNode<Expr.Safe> {
    override fun getChildren(): List<INode> = listOf(term)
    override fun toString(): String = "safe $term"

    override fun getExpression(env: Env): Expr.Safe
        = Expr.Safe(term.getExpression(env))
}
