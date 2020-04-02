package org.orbit.core.nodes

data class MethodSignatureNode(
	val identifierNode: IdentifierNode,
	val receiverTypeNode: PairNode,
	val parameterNodes: List<PairNode>,
	val returnTypeNode: TypeIdentifierNode
) : Node() {
	constructor(
		identifierNode: IdentifierNode,
		receiverTypeNode: TypeIdentifierNode,
		parameterNodes: List<PairNode>,
		returnTypeNode: TypeIdentifierNode
	) : this(
		identifierNode,
		PairNode(IdentifierNode("Self"), receiverTypeNode),
		parameterNodes,
		returnTypeNode
	)

	override fun getChildren() : List<Node> {
		return listOf(
			identifierNode,
			receiverTypeNode,
			returnTypeNode
		) + parameterNodes
	}
}