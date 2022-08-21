package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class CompoundContextNode(override val firstToken: Token, override val lastToken: Token, val context: ContextLiteralNode, val exprs: List<ContextLiteralNode>) : ContextExprNode() {
    override fun getChildren(): List<Node> = listOf(context) + exprs
    override fun toString(): String = "($context + ${exprs.joinToString(" + ") { it.toString() }})"
}
