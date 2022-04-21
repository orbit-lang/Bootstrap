package org.orbit.core.nodes

import org.orbit.core.components.Token

data class DelimitedNode<N: Node>(
    override val firstToken: Token,
    override val lastToken: Token,
    val nodes: List<N>
) : Node() {
    override fun getChildren(): List<Node> = nodes
}