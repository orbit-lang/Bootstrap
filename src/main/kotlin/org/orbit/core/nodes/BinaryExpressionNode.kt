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
) : ExpressionNode(firstToken, lastToken), Serial {
	override fun getChildren() : List<Node> {
		return listOf(left, right)
	}

	override fun describe(json: JSONObject) {
		val l = Serialiser.serialise(left as Serial)
		val r = Serialiser.serialise(right as Serial)

		json.put("OP", operator)
		json.put("LEFT", l)
		json.put("RIGHT", r)
	}
}

data class UnaryExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val operator: String,
    val operand: ExpressionNode
) : ExpressionNode(firstToken, lastToken), Serial {
	override fun getChildren(): List<Node> {
		return listOf(operand)
	}
}