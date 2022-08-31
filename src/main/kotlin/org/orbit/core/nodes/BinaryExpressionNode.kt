package org.orbit.core.nodes

import org.orbit.core.components.Token

data class BinaryExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val operator: String,
    val left: ExpressionNode,
    val right: ExpressionNode
) : ExpressionNode, ValueRepresentableNode {
	override fun getChildren() : List<INode> {
		return listOf(left, right)
	}
}

data class UnaryExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val operator: String,
    val operand: ExpressionNode,
    val fixity: OperatorFixity
) : ExpressionNode {
	override fun getChildren(): List<INode> {
		return listOf(operand)
	}
}