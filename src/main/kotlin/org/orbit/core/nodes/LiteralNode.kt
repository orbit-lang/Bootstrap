package org.orbit.core.nodes

import org.json.JSONObject
import org.orbit.core.components.Token
import org.orbit.serial.Serial

interface ValueRepresentableNode : INode

interface LiteralNode<T> : ConstantExpressionNode, Serial, ValueRepresentableNode {
    val value: T

	override fun getChildren() : List<INode> {
		return emptyList()
	}

	override fun describe(json: JSONObject) {
		json.put("LITERAL", value)
	}
}

data class IntLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: Pair<Int, Int>
) : LiteralNode<Pair<Int, Int>> {
	constructor(f: Token, l: Token, width: Int, value: Int)
		: this(f, l, Pair(width, value))

	// Ints are 32 bit by default
	constructor(f: Token, l: Token, value: Int)
		: this(f, l, Pair(32, value))
}

data class RealLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: Double
) : LiteralNode<Double>

data class BoolLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: Boolean
) : LiteralNode<Boolean>

data class SymbolLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: Pair<Int, String>
) : LiteralNode<Pair<Int, String>> {
	constructor(f: Token, l: Token, length: Int, value: String)
		: this(f, l, Pair(length, value))
}
