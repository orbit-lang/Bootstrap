package org.orbit.core.nodes

import org.orbit.core.Token

data class MethodSignatureNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val identifierNode: IdentifierNode,
	val receiverTypeNode: PairNode,
	val parameterNodes: List<PairNode>,
	val returnTypeNode: TypeIdentifierNode?
) : Node(firstToken, lastToken) {
	constructor(
		firstToken: Token,
		lastToken: Token,
		identifierNode: IdentifierNode,
		receiverTypeNode: TypeIdentifierNode,
		parameterNodes: List<PairNode>,
		returnTypeNode: TypeIdentifierNode?
	) : this(
		firstToken,
		lastToken,
		identifierNode,
		PairNode(receiverTypeNode.firstToken, receiverTypeNode.lastToken,
			IdentifierNode(receiverTypeNode.firstToken, receiverTypeNode.lastToken, "Self"), receiverTypeNode),
		parameterNodes,
		returnTypeNode
	)

	override fun getChildren() : List<Node> = when (returnTypeNode) {
		null -> listOf(identifierNode, receiverTypeNode) + parameterNodes
		else -> listOf(identifierNode, receiverTypeNode, returnTypeNode) + parameterNodes
	}
}