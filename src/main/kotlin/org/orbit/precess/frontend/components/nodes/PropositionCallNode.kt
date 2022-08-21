package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class PropositionCallNode(override val firstToken: Token, override val lastToken: Token, val propId: String, val context: ContextLiteralNode) : ExprNode() {
    override fun getChildren(): List<Node> = listOf(context)
    override fun toString(): String = "$propId($context)"
}
