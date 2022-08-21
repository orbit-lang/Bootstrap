package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class WeakenNode<D>(override val firstToken: Token, override val lastToken: Token, val context: ContextLiteralNode, val decl: D) : ContextExprNode() where D : DeclNode {
    override fun getChildren(): List<Node> = listOf(context, decl)
    override fun toString(): String = "($context + $decl)"
}
