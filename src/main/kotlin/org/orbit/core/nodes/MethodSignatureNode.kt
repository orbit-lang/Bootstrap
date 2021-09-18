package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.graph.components.Annotations
import org.orbit.graph.extensions.annotate
import org.orbit.serial.Serial
import java.io.Serializable

data class MethodSignatureNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifierNode: IdentifierNode,
    val receiverTypeNode: PairNode,
    val parameterNodes: List<PairNode>,
    val returnTypeNode: TypeExpressionNode?,
	val typeParameters: TypeParametersNode? = null
) : Node(firstToken, lastToken) {
	constructor(
        firstToken: Token,
        lastToken: Token,
        identifierNode: IdentifierNode,
        receiverTypeNode: TypeExpressionNode,
        parameterNodes: List<PairNode>,
        returnTypeNode: TypeExpressionNode?,
		typeParameterNodes: TypeParametersNode? = null
	) : this(
		firstToken,
		lastToken,
		identifierNode,
		PairNode(receiverTypeNode.firstToken, receiverTypeNode.lastToken,
			IdentifierNode(receiverTypeNode.firstToken, receiverTypeNode.lastToken, "Self"), receiverTypeNode),
		parameterNodes,
		returnTypeNode,
		typeParameterNodes,
	)

	inline fun <reified T> annotateParameter(idx: Int, value: T, tag: Annotations) where T: Serial, T: Serializable {
		parameterNodes[idx].annotate(value, tag)
	}

	val hasInstanceReceiver: Boolean
		get() = receiverTypeNode.identifierNode.identifier != "Self"

	override fun getChildren() : List<Node> = when (returnTypeNode) {
		null -> listOf(identifierNode, receiverTypeNode) + parameterNodes
		else -> listOf(identifierNode, receiverTypeNode, returnTypeNode) + parameterNodes
	}
}