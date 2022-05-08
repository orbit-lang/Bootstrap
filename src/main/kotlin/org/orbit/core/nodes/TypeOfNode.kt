package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeOfNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val expressionNode: ExpressionNode
) : Node() {
    override fun getChildren(): List<Node>
        = listOf(expressionNode)
}
