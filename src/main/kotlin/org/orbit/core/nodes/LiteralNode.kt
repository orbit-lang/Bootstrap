package org.orbit.core.nodes

import org.orbit.core.Token
import java.math.BigInteger

// NOTE: Literals might work better as annotations controlled
// and owned by a phase, rather than baked in like this
abstract class LiteralNode<T>(
	override val firstToken: Token,
	override val lastToken: Token,
	open val value: T
) : ExpressionNode(firstToken, lastToken) {
	override fun getChildren() : List<Node> {
		return emptyList()
	}
}

data class IntLiteralNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val value: Pair<Int, BigInteger>
) : LiteralNode<Pair<Int, BigInteger>>(firstToken, lastToken, value) {
	constructor(f: Token, l: Token, width: Int, value: BigInteger)
		: this(f, l, Pair(width, value))

	// Ints are 32 bit by default
	// TODO - We could be clever and set the width
	// to be the smallest power of 2 that fits `value`
	constructor(f: Token, l: Token, value: BigInteger)
		: this(f, l, Pair(32, value))
}

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

data class SymbolLiteralNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val value: Pair<Int, String>
) : LiteralNode<Pair<Int, String>>(firstToken, lastToken, value) {
	constructor(f: Token, l: Token, length: Int, value: String)
		: this(f, l, Pair(length, value))
}