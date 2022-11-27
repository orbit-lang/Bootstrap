package org.orbit.core.nodes

import org.orbit.core.components.Token

data class RefOfNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val ref: IdentifierNode
) : IMethodBodyStatementNode {
    override fun getChildren(): List<INode> = listOf(ref)
}
