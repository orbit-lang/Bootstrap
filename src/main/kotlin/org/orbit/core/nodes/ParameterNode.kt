package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ParameterNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifierNode: IdentifierNode,
    val typeNode: TypeExpressionNode,
    val defaultValue: ExpressionNode?
) : Node() {
    override fun getChildren(): List<Node> = when (defaultValue) {
        null -> listOf(identifierNode, typeNode)
        else -> listOf(identifierNode, typeNode, defaultValue)
    }

    fun toPairNode() : PairNode
        = PairNode(firstToken, lastToken, identifierNode, typeNode)
}
