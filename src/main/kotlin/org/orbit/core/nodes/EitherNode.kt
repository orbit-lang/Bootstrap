package org.orbit.core.nodes

import org.orbit.core.components.Token

data class EitherNode<N: Node, M: Node>(
    override val firstToken: Token,
    override val lastToken: Token,
    val leftNode: N?,
    val rightNode: M?
) : Node(firstToken, lastToken) {
    val isLeft: Boolean get() = leftNode != null
    val isRight: Boolean get() = rightNode != null

    override fun getChildren(): List<Node> = when (leftNode) {
        null -> listOf(rightNode!!)
        else -> listOf(leftNode)
    }
}