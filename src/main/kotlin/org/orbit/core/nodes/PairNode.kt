package org.orbit.core.nodes

import org.orbit.core.components.Token

data class PairNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifierNode: IdentifierNode,
    val typeExpressionNode: TypeExpressionNode
) : INode {
	override fun getChildren() : List<INode> {
		return listOf(identifierNode, typeExpressionNode)
	}
}