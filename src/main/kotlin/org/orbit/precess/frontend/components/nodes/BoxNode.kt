package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr

data class BoxNode(override val firstToken: Token, override val lastToken: Token, val term: LookupNode<*>) : TermExpressionNode<Expr.Box>() {
    override fun getChildren(): List<Node> = listOf(term)
    override fun toString(): String = "⎡$term⎦"

    override fun getExpression(env: Env): Expr.Box
        = Expr.Box(term.getExpression(env))
}