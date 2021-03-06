package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TraitDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val isRequired: Boolean,
    val typeIdentifierNode: TypeIdentifierNode,
    val propertyPairs: List<PairNode> = emptyList(),
    val traitConformances: List<TypeIdentifierNode> = emptyList(),
    val signatures: List<MethodSignatureNode> = emptyList()
) : EntityDefNode(firstToken, lastToken, isRequired) {
	override fun getChildren() : List<Node>
		= listOf(typeIdentifierNode) + traitConformances + propertyPairs + signatures
}