package org.orbit.core.nodes

import org.orbit.core.components.Token

data class PairNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifierNode: IdentifierNode,
    val typeExpressionNode: TypeExpressionNode
) : IContextVariableNode {
    override val value: String = typeExpressionNode.value
    override fun getTypeName(): String = typeExpressionNode.getTypeName()

	override fun getChildren() : List<INode> {
		return listOf(identifierNode, typeExpressionNode)
	}
}