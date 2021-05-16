package org.orbit.core.nodes

import org.orbit.core.components.Token

data class PrintNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val expressionNode: ExpressionNode
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(expressionNode)
    }
}