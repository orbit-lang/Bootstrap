package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeAliasNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val sourceTypeIdentifier: TypeIdentifierNode,
    val targetTypeIdentifier: TypeExpressionNode
) : IContextDeclarationNode {
    override fun getChildren(): List<INode>
        = listOf(sourceTypeIdentifier, targetTypeIdentifier)
}