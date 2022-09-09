package org.orbit.core.nodes

import org.orbit.core.components.Token

interface ConstantExpressionNode : IExpressionNode

data class ExpandNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val expressionNode: IExpressionNode
) : TypeExpressionNode {
    override val value: String = ""
    override fun getChildren(): List<INode> = listOf(expressionNode)
}
