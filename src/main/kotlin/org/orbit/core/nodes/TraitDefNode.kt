package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TraitDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val typeIdentifierNode: TypeIdentifierNode,
    override val properties: List<ParameterNode> = emptyList(),
    val traitConformances: List<TypeIdentifierNode> = emptyList(),
    val signatures: List<MethodSignatureNode> = emptyList()
) : EntityDefNode, IContextDeclarationNode {
	override fun getChildren() : List<INode>
		= listOf(typeIdentifierNode) + traitConformances + properties + signatures
}