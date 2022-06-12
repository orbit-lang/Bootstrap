package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ByExpressionNode<N: Node>(
    override val firstToken: Token,
    override val lastToken: Token,
    val rhs: N
) : Node() {
    override fun getChildren(): List<Node> = listOf(rhs)
}
