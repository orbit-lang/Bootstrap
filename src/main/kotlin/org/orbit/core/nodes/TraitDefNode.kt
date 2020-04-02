package org.orbit.core.nodes

data class TraitDefNode(
	val typeIdentifierNode: TypeIdentifierNode,
	val propertyPairs: List<PairNode> = emptyList(),
	val signatures: List<MethodSignatureNode> = emptyList()
) : Node() {
	override fun getChildren() : List<Node> {
		return listOf(typeIdentifierNode) + signatures + propertyPairs
	}
}