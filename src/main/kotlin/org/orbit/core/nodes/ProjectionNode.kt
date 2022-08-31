package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ProjectionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeIdentifier: TypeExpressionNode,
    val traitIdentifier: TypeExpressionNode,
    val whereNodes: List<WhereClauseNode> = emptyList(),
    val instanceBinding: IdentifierNode?
) : INode {
    override fun getChildren(): List<INode> = when (instanceBinding) {
        null -> listOf(typeIdentifier, traitIdentifier) + whereNodes
        else -> listOf(typeIdentifier, traitIdentifier) + whereNodes + instanceBinding
    }
}