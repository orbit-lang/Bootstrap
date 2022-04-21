package org.orbit.core.nodes

import org.orbit.core.components.Token

data class PairNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifierNode: IdentifierNode,
    val typeExpressionNode: TypeExpressionNode
) : Node() {
	override fun getChildren() : List<Node> {
		return listOf(identifierNode, typeExpressionNode)
	}
}