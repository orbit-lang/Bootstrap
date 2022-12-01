package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeAliasNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val sourceTypeIdentifier: TypeIdentifierNode,
    val targetType: TypeExpressionNode
) : IContextDeclarationNode, EntityDefNode {
    override val properties: List<ParameterNode> = emptyList()
    override val typeIdentifierNode: TypeIdentifierNode = sourceTypeIdentifier

    override fun getChildren(): List<INode>
        = listOf(sourceTypeIdentifier, targetType)
}