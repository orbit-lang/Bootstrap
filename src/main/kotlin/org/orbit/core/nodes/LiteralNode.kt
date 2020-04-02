package org.orbit.core.nodes

// NOTE: Literals might work better as annotations controlled
// and owned by a phase, rather than baked in like this
abstract class LiteralNode<T>(open val value: T) : Node() {
	override fun getChildren() : List<Node> {
		return emptyList()
	}
}

data class IntegerLiteralNode(override val value: Int)
	: LiteralNode<Int>(value)

data class RealLiteralNode(override val value: Double)
	: LiteralNode<Double>(value)

data class BoolLiteralNode(override val value: Boolean)
	: LiteralNode<Boolean>(value)