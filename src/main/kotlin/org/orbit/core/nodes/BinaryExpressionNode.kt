package org.orbit.core.nodes

data class BinaryExpressionNode(
	val left: Node,
	val right: Node
) : Node()