package org.orbit.core.nodes

import org.orbit.core.components.Token

sealed interface ISignatureRepresentableNode : INode

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
	val effects: List<EffectDeclarationNode> = emptyList()
) : ISignatureRepresentableNode {
	inline fun <reified T> annotateParameter(idx: Int, value: T, tag: Annotations<T>) {}

	override fun getChildren() : List<INode> = when (returnTypeNode) {
		null -> listOf(identifierNode, receiverTypeNode) + parameterNodes + typeConstraints + effects
		else -> listOf(identifierNode, receiverTypeNode, returnTypeNode) + parameterNodes + typeConstraints + effects
	}

	fun getAllParameterPairs() : List<PairNode> = parameterNodes
}