package org.orbit.core.nodes

import org.orbit.core.Token

// NOTE: Literals might work better as annotations controlled
// and owned by a phase, rather than baked in like this
abstract class LiteralNode<T>(
	override val firstToken: Token,
	override val lastToken: Token,
	open val value: T
) : Node(firstToken, lastToken) {
	override fun getChildren() : List<Node> {
		return emptyList()
	}
}

data class IntegerLiteralNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val value: Int
) : LiteralNode<Int>(firstToken, lastToken, value)

data class RealLiteralNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val value: Double
) : LiteralNode<Double>(firstToken, lastToken, value)

data class BoolLiteralNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val value: Boolean
) : LiteralNode<Boolean>(firstToken, lastToken, value)