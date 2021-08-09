package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeAliasNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val sourceTypeIdentifier: TypeIdentifierNode,
    val targetTypeIdentifier: TypeExpressionNode
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(sourceTypeIdentifier, targetTypeIdentifier)
    }
}