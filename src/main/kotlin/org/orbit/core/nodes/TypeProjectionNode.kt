package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeProjectionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeIdentifier: TypeExpressionNode,
    val traitIdentifier: TypeExpressionNode,
    val whereNodes: List<WhereClauseNode> = emptyList()
) : Node() {
    override fun getChildren(): List<Node> = listOf(typeIdentifier, traitIdentifier) + whereNodes
}