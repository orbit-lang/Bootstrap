package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class RefExprNode(override val firstToken: Token, override val lastToken: Token, val context: ContextLiteralNode, val ref: RefLiteralNode) : ExprNode() {
    override fun getChildren(): List<Node> = listOf(context, ref)
    override fun toString(): String = "$context.$ref"
}