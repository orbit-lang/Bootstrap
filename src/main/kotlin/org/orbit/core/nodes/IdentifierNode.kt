package org.orbit.core.nodes

data class IdentifierNode(val identifier: String) : Node() {
	override fun getChildren() : List<Node> {
		return emptyList()
	}
}