package org.orbit.core.nodes

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
	val isInstanceMethod: Boolean,
	val receiverIdentifier: IdentifierNode?,
	val effects: List<TypeIdentifierNode> = emptyList()
) : INode {
	inline fun <reified T> annotateParameter(idx: Int, value: T, tag: Annotations<T>) {}

	override fun getChildren() : List<INode> = when (returnTypeNode) {
		null -> listOf(identifierNode, receiverTypeNode) + parameterNodes + typeConstraints + effects
		else -> listOf(identifierNode, receiverTypeNode, returnTypeNode) + parameterNodes + typeConstraints + effects
	}

	fun getAllParameters() : List<TypeExpressionNode> = parameterNodes.map { it.typeExpressionNode }

	fun getAllParameterPairs() : List<PairNode> = parameterNodes
		//when (isInstanceMethod) {
//		// TODO - "self" name needs to be captured in MethodSignatureRule
//		true -> listOf(PairNode(Token.empty, Token.empty, IdentifierNode(Token.empty, Token.empty, "self"), receiverTypeNode)) + parameterNodes
//		else -> parameterNodes
//	}
}