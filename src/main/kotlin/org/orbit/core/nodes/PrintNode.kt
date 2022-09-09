package org.orbit.core.nodes

import org.orbit.core.components.Token

data class PrintNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val expressionNode: IExpressionNode
) : INode {
    override fun getChildren(): List<INode>
        = listOf(expressionNode)
}