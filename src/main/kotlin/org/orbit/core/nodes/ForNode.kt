package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ForNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val iterable: IExpressionNode,
    val body: IExpressionNode
) : IExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(iterable, body)
}
