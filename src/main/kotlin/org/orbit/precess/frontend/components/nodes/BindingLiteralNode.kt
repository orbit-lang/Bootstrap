package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class BindingLiteralNode(override val firstToken: Token, override val lastToken: Token, val ref: RefLiteralNode, val type: TypeExprNode) : Node() {
    override fun getChildren(): List<Node> = listOf(ref, type)
    override fun toString(): String = "$ref:$type"
}
