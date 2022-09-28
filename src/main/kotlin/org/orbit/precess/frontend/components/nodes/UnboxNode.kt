package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.backend.typesystem.components.Env
import org.orbit.precess.backend.components.Expr

data class UnboxNode(override val firstToken: Token, override val lastToken: Token, val box: TermExpressionNode<*>) : TermExpressionNode<Expr.Unbox> {
    override fun getChildren(): List<INode> = listOf(box)
    override fun toString(): String = "$box"

    override fun getExpression(env: Env): Expr.Unbox
        = Expr.Unbox(box.getExpression(env))
}
