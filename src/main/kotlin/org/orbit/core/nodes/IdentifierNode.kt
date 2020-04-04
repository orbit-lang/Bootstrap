package org.orbit.core.nodes

import org.orbit.core.Token

data class IdentifierNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val identifier: String
) : Node(firstToken, lastToken) {
	override fun getChildren() : List<Node> {
		return emptyList()
	}
}