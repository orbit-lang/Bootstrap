package org.orbit.core.nodes

import org.orbit.backend.typesystem.intrinsics.OrbCoreBooleans
import org.orbit.backend.typesystem.intrinsics.OrbCoreNumbers
import org.orbit.backend.typesystem.intrinsics.OrbCoreStrings
import org.orbit.core.OrbitMangler
import org.orbit.core.components.Token

interface ValueRepresentableNode : INode

interface ILiteralNode<T> : IConstantExpressionNode, ValueRepresentableNode, IPatternNode {
    val value: T

	override fun getChildren() : List<INode> = emptyList()
}

data class IntLiteralNode(override val firstToken: Token, override val lastToken: Token, override val value: Pair<Int, Int>) : ILiteralNode<Pair<Int, Int>> {
	// Ints are 32 bit by default
	constructor(f: Token, l: Token, value: Int) : this(f, l, Pair(32, value))

    override fun getTypeName(): String = OrbCoreNumbers.intType.getCanonicalName()
}

data class RealLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: Double
) : ILiteralNode<Double> {
    // TODO - Make Double
    override fun getTypeName(): String = OrbCoreNumbers.intType.getCanonicalName()
}

data class BoolLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: Boolean
) : ILiteralNode<Boolean> {
    override fun getTypeName(): String = when (value) {
        true -> "True"
        else -> "False"
    }
}

data class SymbolLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: Pair<Int, String>
) : ILiteralNode<Pair<Int, String>> {
	constructor(f: Token, l: Token, length: Int, value: String)
		: this(f, l, Pair(length, value))

    // TODO - Make Symbol
    override fun getTypeName(): String = OrbCoreNumbers.intType.getPath().toString(OrbitMangler)
}

data class StringLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val text: String
) : ILiteralNode<String> {
    override val value: String = text
    override fun getTypeName(): String = OrbCoreStrings.stringType.getCanonicalName()
}

