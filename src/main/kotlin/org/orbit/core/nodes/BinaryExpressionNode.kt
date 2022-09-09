package org.orbit.core.nodes

import org.orbit.core.components.Token

data class BinaryExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val operator: String,
    val left: IExpressionNode,
    val right: IExpressionNode
) : IExpressionNode, ValueRepresentableNode {
	override fun getChildren() : List<INode> {
		return listOf(left, right)
	}
}

data class UnaryExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val operator: String,
    val operand: IExpressionNode,
    val fixity: OperatorFixity
) : IExpressionNode {
	override fun getChildren(): List<INode> {
		return listOf(operand)
	}
}