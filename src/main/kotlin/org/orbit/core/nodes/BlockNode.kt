package org.orbit.core.nodes

data class BlockNode(val body: Array<Node>) : Node() {
	override fun getChildren() : List<Node> {
		return body.toList()
	}
}