package org.orbit.core.nodes

import org.orbit.core.components.Token

data class DeferNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val returnValueIdentifier: IdentifierNode?,
    val blockNode: BlockNode
) : Node(firstToken, lastToken), ScopedNode {
    override fun getChildren(): List<Node> {
        return when (returnValueIdentifier) {
            null -> listOf(blockNode)
            else -> listOf(returnValueIdentifier, blockNode)
        }
    }
}