package org.orbit.core.nodes

import org.orbit.core.Token

data class PairNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val identifierNode: IdentifierNode,
	val typeIdentifierNode: TypeIdentifierNode
) : Node(firstToken, lastToken) {
	override fun getChildren() : List<Node> {
		return listOf(identifierNode, typeIdentifierNode)
	}
}