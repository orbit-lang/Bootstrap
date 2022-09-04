package org.orbit.core.nodes

import org.orbit.core.components.Token

interface EntityDefNode : INode {
    val properties: List<ParameterNode>
    val typeIdentifierNode: TypeIdentifierNode
}

interface ITypeDefBodyNode : INode

class TypeDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val properties: List<ParameterNode> = emptyList(),
    val traitConformances: List<TypeExpressionNode> = emptyList(),
    val body: List<ITypeDefBodyNode> = emptyList(),
) : EntityDefNode {
    override fun getChildren() : List<INode>
		= listOf(typeIdentifierNode) + traitConformances + properties + body
}