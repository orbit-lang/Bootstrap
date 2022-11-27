package org.orbit.core.nodes

import org.orbit.core.components.Token

data class DelimitedNode<N: INode>(
    override val firstToken: Token,
    override val lastToken: Token,
    val nodes: List<N>
) : INode {
    override fun getChildren(): List<INode> = nodes
}

data class SeparatedNode<N: INode>(
    override val firstToken: Token,
    override val lastToken: Token,
    val nodes: List<N>
) : INode {
    override fun getChildren(): List<INode> = nodes
}