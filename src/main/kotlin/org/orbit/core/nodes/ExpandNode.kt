package org.orbit.core.nodes

import org.orbit.core.components.Token

abstract class ConstantExpressionNode : ExpressionNode()

data class ExpandNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val expressionNode: ExpressionNode
) : ConstantExpressionNode() {
    override fun getChildren(): List<Node> = listOf(expressionNode)
}
