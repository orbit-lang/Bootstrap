package org.orbit.core.nodes

import org.orbit.core.components.Token

interface ValueRepresentableNode : INode

interface ILiteralNode<T> : ConstantExpressionNode, ValueRepresentableNode, IPatternNode {
    val value: T

	override fun getChildren() : List<INode> = emptyList()
}

data class IntLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: Pair<Int, Int>
) : ILiteralNode<Pair<Int, Int>> {
	// Ints are 32 bit by default
	constructor(f: Token, l: Token, value: Int)
		: this(f, l, Pair(32, value))
}

data class RealLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: Double
) : ILiteralNode<Double>

data class BoolLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: Boolean
) : ILiteralNode<Boolean>

data class SymbolLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: Pair<Int, String>
) : ILiteralNode<Pair<Int, String>> {
	constructor(f: Token, l: Token, length: Int, value: String)
		: this(f, l, Pair(length, value))
}
