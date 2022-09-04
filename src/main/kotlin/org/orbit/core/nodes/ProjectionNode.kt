package org.orbit.core.nodes

import org.orbit.core.components.Token

interface IProjectionDeclarationNode : INode

data class ProjectionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeIdentifier: TypeExpressionNode,
    val traitIdentifier: TypeExpressionNode,
    val whereNodes: List<WhereClauseNode> = emptyList(),
    val instanceBinding: IdentifierNode?,
    val body: List<IProjectionDeclarationNode> = emptyList(),
    override val context: ContextExpressionNode? = null
) : IExtensionDeclarationNode, IWithStatementNode, ContextAwareNode {
    override fun getChildren(): List<INode> = when (instanceBinding) {
        null -> listOf(typeIdentifier, traitIdentifier) + whereNodes + body
        else -> listOf(typeIdentifier, traitIdentifier) + whereNodes + body + instanceBinding
    }
}