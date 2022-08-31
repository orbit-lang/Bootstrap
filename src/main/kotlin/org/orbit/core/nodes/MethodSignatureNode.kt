package org.orbit.core.nodes

import org.orbit.core.AnySerializable
import org.orbit.core.components.Token

data class MethodSignatureNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifierNode: IdentifierNode,
    val receiverTypeNode: TypeExpressionNode,
    val parameterNodes: List<PairNode>,
    val returnTypeNode: TypeExpressionNode?,
	val typeParameters: TypeParametersNode? = null,
	val typeConstraints: List<TypeConstraintWhereClauseNode> = emptyList(),
	val isInstanceMethod: Boolean
) : INode {
	inline fun <reified T: AnySerializable> annotateParameter(idx: Int, value: T, tag: Annotations) {
		// TODO
//		val nodeAnnotationMap = getKoinInstance<NodeAnnotationMap>()
//
		//nodeAnnotationMap.annotate(parameterNodes[idx], value)
		//parameterNodes[idx].annotate(value, tag)
	}

	override fun getChildren() : List<INode> = when (returnTypeNode) {
		null -> listOf(identifierNode, receiverTypeNode) + parameterNodes + typeConstraints
		else -> listOf(identifierNode, receiverTypeNode, returnTypeNode) + parameterNodes + typeConstraints
	}
}