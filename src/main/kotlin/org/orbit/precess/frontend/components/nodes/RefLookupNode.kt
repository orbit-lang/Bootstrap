package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr

sealed class LookupNode<E: Expr<E>> : TermExpressionNode<E>()

data class RefLookupNode(override val firstToken: Token, override val lastToken: Token, val context: ContextLiteralNode, val ref: RefLiteralNode) : LookupNode<Expr.Var>() {
    override fun getChildren(): List<Node> = listOf(context, ref)
    override fun toString(): String = "$ref"

    override fun getExpression(env: Env): Expr.Var
        = Expr.Var(ref.refId)
}

