package org.orbit.core.nodes

import org.orbit.core.components.Token

data class CollectionLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val elements: List<IExpressionNode>
) : IExpressionNode {
    override fun getChildren(): List<INode> = elements
}