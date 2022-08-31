package org.orbit.core.nodes

import org.orbit.core.components.Token

interface IExtensionDeclarationNode : INode

data class ExtensionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val targetTypeNode: TypeExpressionNode,
    val bodyNodes: List<IExtensionDeclarationNode> = emptyList(),
    override val context: ContextExpressionNode? = null
) : ContextAwareNode {
    override fun getChildren(): List<INode> = when (context) {
        null -> listOf(targetTypeNode) + bodyNodes
        else -> listOf(targetTypeNode) + bodyNodes + context
    }
}