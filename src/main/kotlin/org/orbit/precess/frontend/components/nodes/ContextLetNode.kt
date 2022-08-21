package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

abstract class StatementNode : Node()

data class ContextLetNode(override val firstToken: Token, override val lastToken: Token, val nContext: ContextLiteralNode, val enclosing: ContextLiteralNode, val expr: ContextExprNode) : StatementNode() {
    override fun getChildren(): List<Node> = listOf(nContext, enclosing, expr)
    override fun toString(): String = "$nContext = $enclosing => $expr"
}
