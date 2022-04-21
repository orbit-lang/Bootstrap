package org.orbit.core.nodes

import org.json.JSONObject
import org.orbit.core.components.Token
import org.orbit.serial.Serial
import org.orbit.serial.Serialiser

data class BinaryExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val operator: String,
    val left: ExpressionNode,
    val right: ExpressionNode
) : ExpressionNode() {
	override fun getChildren() : List<Node> {
		return listOf(left, right)
	}
}

data class UnaryExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val operator: String,
    val operand: ExpressionNode
) : ExpressionNode() {
	override fun getChildren(): List<Node> {
		return listOf(operand)
	}
}