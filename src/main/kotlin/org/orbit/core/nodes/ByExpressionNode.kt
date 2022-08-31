package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ByExpressionNode<N: INode>(
    override val firstToken: Token,
    override val lastToken: Token,
    val rhs: N
) : INode {
    override fun getChildren(): List<INode> = listOf(rhs)
}
