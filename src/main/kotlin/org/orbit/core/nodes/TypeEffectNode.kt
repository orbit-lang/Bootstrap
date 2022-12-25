package org.orbit.core.nodes

import org.orbit.core.components.Token

sealed interface ITypeEffectExpressionNode : INode

data class ProjectionEffectNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val type: TypeIdentifierNode,
    val trait: TypeIdentifierNode
) : ITypeEffectExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(type, trait)
}

data class TypeEffectNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: TypeIdentifierNode,
    val parameters: List<TypeIdentifierNode>,
    val body: ITypeEffectExpressionNode
) : TopLevelDeclarationNode {
    override val context: IContextExpressionNode? = null

    override fun getChildren(): List<INode>
        = listOf(identifier) + parameters + body
}
