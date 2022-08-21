package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class CheckNode(override val firstToken: Token, override val lastToken: Token, val context: ContextLiteralNode, val lhs: ExprNode, val rhs: TypeExprNode) : ExprNode() {
    override fun getChildren(): List<Node> = listOf(context, lhs, rhs)
    override fun toString(): String = "check($lhs, $rhs) in $context"
}
