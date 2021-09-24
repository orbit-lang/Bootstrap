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
    val receiverTypeNode: TypeExpressionNode,
    val parameterNodes: List<PairNode>,
    val returnTypeNode: TypeExpressionNode?,
	val typeParameters: TypeParametersNode? = null,
	val typeConstraints: List<TypeConstraintWhereClauseNode> = emptyList()
) : Node(firstToken, lastToken) {
	inline fun <reified T> annotateParameter(idx: Int, value: T, tag: Annotations) where T: Serial, T: Serializable {
		parameterNodes[idx].annotate(value, tag)
	}

	override fun getChildren() : List<Node> = when (returnTypeNode) {
		null -> listOf(identifierNode, receiverTypeNode) + parameterNodes + typeConstraints
		else -> listOf(identifierNode, receiverTypeNode, returnTypeNode) + parameterNodes + typeConstraints
	}
}