package org.orbit.core.nodes

import org.orbit.core.Token

data class TraitDefNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val typeIdentifierNode: TypeIdentifierNode,
	val propertyPairs: List<PairNode> = emptyList(),
	val signatures: List<MethodSignatureNode> = emptyList()
) : Node(firstToken, lastToken) {
	override fun getChildren() : List<Node> {
		return listOf(typeIdentifierNode) + signatures + propertyPairs
	}
}