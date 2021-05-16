package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeIdentifierNode: TypeIdentifierNode,
    val parameterNodes: List<ExpressionNode>
) : ExpressionNode(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(typeIdentifierNode) + parameterNodes
    }
}