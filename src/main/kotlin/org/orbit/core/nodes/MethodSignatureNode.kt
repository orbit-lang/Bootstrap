package org.orbit.core.nodes

import org.orbit.core.Token
import org.orbit.graph.Annotations
import org.orbit.graph.annotate
import org.orbit.serial.Serial

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

	inline fun <reified T: Serial> annotateParameter(idx: Int, value: T, tag: Annotations) {
		parameterNodes[idx].annotate(value, tag)
	}

	override fun getChildren() : List<Node> = when (returnTypeNode) {
		null -> listOf(identifierNode, receiverTypeNode) + parameterNodes
		else -> listOf(identifierNode, receiverTypeNode, returnTypeNode) + parameterNodes
	}
}