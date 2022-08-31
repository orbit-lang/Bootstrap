package org.orbit.core.nodes

import org.orbit.core.components.Token

data class EitherNode<N: INode, M: INode>(
    override val firstToken: Token,
    override val lastToken: Token,
    val leftNode: N?,
    val rightNode: M?
) : INode {
    val isLeft: Boolean get() = leftNode != null
    val isRight: Boolean get() = rightNode != null

    override fun getChildren(): List<INode> = when (leftNode) {
        null -> listOf(rightNode!!)
        else -> listOf(leftNode)
    }
}