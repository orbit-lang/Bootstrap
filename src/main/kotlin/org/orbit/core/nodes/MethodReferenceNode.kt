package org.orbit.core.nodes

import org.orbit.core.components.Token

data class MethodReferenceNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeIdentifierNode: TypeIdentifierNode,
    val identifierNode: IdentifierNode
) : ExpressionNode() {
    override fun getChildren(): List<Node> {
        return listOf(typeIdentifierNode, identifierNode)
    }
}