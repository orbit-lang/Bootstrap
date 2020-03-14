package org.orbit.core.nodes

data class PairNode(
	val identifierNode: IdentifierNode,
	val typeIdentifierNode: TypeIdentifierNode
) : Node()