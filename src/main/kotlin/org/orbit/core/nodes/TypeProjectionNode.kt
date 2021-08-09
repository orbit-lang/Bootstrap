package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeProjectionNode(
    val typeIdentifier: TypeIdentifierNode,
    val traitIdentifier: TypeIdentifierNode,
    override val firstToken: Token,
    override val lastToken: Token
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> = listOf(typeIdentifier, traitIdentifier)
}