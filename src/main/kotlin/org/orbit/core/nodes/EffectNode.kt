package org.orbit.core.nodes

import org.orbit.core.components.Token

data class EffectNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: TypeIdentifierNode,
    val lambda: LambdaTypeNode
) : IContextDeclarationNode, EntityDefNode {
    override val properties: List<ParameterNode> = emptyList()
    override val typeIdentifierNode: TypeIdentifierNode = identifier

    override fun getChildren(): List<INode>
        = listOf(identifier, lambda)
}
