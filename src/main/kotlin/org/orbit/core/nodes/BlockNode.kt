package org.orbit.core.nodes

import org.orbit.core.Token

data class BlockNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val body: List<Node>) : Node(firstToken, lastToken) {
	override fun getChildren() : List<Node> {
		return body
	}
}