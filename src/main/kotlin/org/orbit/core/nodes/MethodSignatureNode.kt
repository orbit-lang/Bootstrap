package org.orbit.core.nodes

data class MethodSignatureNode(
	val identifierNode: IdentifierNode,
	val receiverTypeNode: TypeIdentifierNode,
	val parameterNodes: Array<PairNode>,
	val returnTypeNode: TypeIdentifierNode
) : Node()