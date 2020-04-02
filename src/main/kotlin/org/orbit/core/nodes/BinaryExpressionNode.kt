package org.orbit.core.nodes

data class BinaryExpressionNode(
	val left: Node,
	val right: Node
) : Node() {
	override fun getChildren() : List<Node> {
		return listOf(left, right)
	}
}