package org.orbit.core.nodes

import org.orbit.core.components.Token

data class BlockNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val body: List<Node>) : Node(firstToken, lastToken) {
	val isEmpty: Boolean get() = body.isEmpty()

	override fun getChildren() : List<Node> {
		return body
	}
}