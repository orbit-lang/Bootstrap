package org.orbit.core.nodes

data class PairNode(
	val identifierNode: IdentifierNode,
	val typeIdentifierNode: TypeIdentifierNode
) : Node() {
	override fun getChildren() : List<Node> {
		return listOf(identifierNode, typeIdentifierNode)
	}
}