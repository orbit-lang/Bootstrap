package org.orbit.core.nodes

import org.orbit.core.Token

data class BinaryExpressionNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val left: Node,
	val right: Node
) : Node(firstToken, lastToken) {
	override fun getChildren() : List<Node> {
		return listOf(left, right)
	}
}