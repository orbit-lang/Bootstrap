package org.orbit.core.nodes

import org.orbit.core.components.Token

interface ConstantExpressionNode : ExpressionNode

data class ExpandNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val expressionNode: ExpressionNode
) : TypeExpressionNode {
    override val value: String = ""
    override fun getChildren(): List<INode> = listOf(expressionNode)
}
