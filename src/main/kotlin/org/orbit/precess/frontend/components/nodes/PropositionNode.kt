package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class PropositionNode(override val firstToken: Token, override val lastToken: Token, val propId: String, val enclosing: ContextLiteralNode, val body: ExprNode) : StatementNode() {
    override fun getChildren(): List<Node> = listOf(enclosing, body)
    override fun toString(): String = "$propId = $enclosing => $body"
}
