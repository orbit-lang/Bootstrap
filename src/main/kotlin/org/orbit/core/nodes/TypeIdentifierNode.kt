package org.orbit.core.nodes

import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.TokenTypes

sealed interface TypeExpressionNode : ILiteralNode<String>

data class StarNode(override val firstToken: Token, override val lastToken: Token) : TypeExpressionNode {
	override val value: String = "Any"
	override fun getTypeName(): String = "Any"
	override fun getChildren(): List<INode> = emptyList()
}

data class NeverNode(override val firstToken: Token, override val lastToken: Token) : TypeExpressionNode {
	override val value: String = "Never"
	override fun getTypeName(): String = "Never"
	override fun getChildren(): List<INode> = emptyList()
}

data class InferNode(
	override val firstToken: Token,
	override val lastToken: Token
) : TypeExpressionNode {
	override val value: String = "_"

	override fun getTypeName(): String {
		TODO("Not yet implemented")
	}
}

data class StructTypeNode(override val firstToken: Token, override val lastToken: Token, val members: List<PairNode>) : TypeExpressionNode {
	override val value: String = members.joinToString(", ")

	override fun getChildren(): List<INode> = members
	override fun getTypeName(): String = "{$value}"
}

data class TupleTypeNode(override val firstToken: Token, override val lastToken: Token, val left: TypeExpressionNode, val right: TypeExpressionNode) : TypeExpressionNode {
	override val value: String = "(${left.value}, ${right.value})"

	override fun getChildren(): List<INode>
		= listOf(left, right)
	override fun getTypeName(): String {
		TODO("Not yet implemented")
	}
}

data class UnitNode(override val firstToken: Token, override val lastToken: Token) : TypeExpressionNode {
	override val value: String = "Unit"

	override fun getChildren(): List<INode> = emptyList()
	override fun getTypeName(): String = "Unit"
}

data class TypeIdentifierNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: String,
    val typeParametersNode: TypeParametersNode = TypeParametersNode(firstToken, lastToken)
) : ITypeLambdaParameterNode, LValueTypeParameter, ITypeConstraintNode, IContextVariableNode {
	companion object {
		private val nullToken = Token(TokenTypes.TypeIdentifier, "AnyType", SourcePosition.unknown)
		private val anyTypeIdentifierNode = TypeIdentifierNode(nullToken, nullToken, "Any")

		fun hole(token: Token) : TypeIdentifierNode
			= TypeIdentifierNode(token, token, "_")

		fun any(): TypeIdentifierNode
			= anyTypeIdentifierNode
	}

	override val name: TypeIdentifierNode = this

	override fun getTypeName(): String = name.value

	val isWildcard: Boolean
		get() = value.endsWith("*")

	val isDiscard: Boolean
		get() = value == "_"

	override fun equals(other: Any?): Boolean = when (other) {
		is TypeIdentifierNode -> value == other.value
		else -> false
	}

	override fun getChildren() : List<INode> {
		return typeParametersNode.typeParameters
	}

	override fun toString() : String
		= "${value}<${typeParametersNode.typeParameters.joinToString(", ") { it.toString() }}>"
}

data class VariadicTypeIdentifierNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val identifier: TypeIdentifierNode
) : ITypeLambdaParameterNode {
	override val value: String = identifier.value
	override fun getTypeName(): String = identifier.getTypeName()

	override fun getChildren(): List<INode>
		= listOf(identifier)
}

sealed interface ITypeSliceNode : TypeExpressionNode {
	val identifier: TypeIdentifierNode

	override fun getChildren(): List<INode> = listOf(identifier)
}

data class IndexSliceNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val identifier: TypeIdentifierNode,
	// TODO - We should be able to generalise this concept with Dependent Types (or maybe Quotient Types?)
	val index: Int
) : ITypeSliceNode {
	override fun getTypeName(): String = "${identifier.getTypeName()}[$index]"
	override val value: String = getTypeName()
}

interface ICaseIterable<Self> {
	fun allCases() : List<Self>
}

interface ICaseConvertible<I, Self> : ICaseIterable<Self> {
	fun fromOrNull(input: I) : Self?
}

fun <I, Self> ICaseConvertible<I, Self>.from(input: I) : Self
	= fromOrNull(input)!!

enum class IRangeOperator(val op: String) {
	Inclusive("..."),
	Exclusive("..");

	companion object : ICaseConvertible<String, IRangeOperator> {
		override fun allCases(): List<IRangeOperator>
			= listOf(Inclusive, Exclusive)

		override fun fromOrNull(input: String) : IRangeOperator? = when (input) {
			Inclusive.op -> Inclusive
			Exclusive.op -> Exclusive
			else -> null
		}
	}

	override fun toString(): String = op
}

data class RangeSliceNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val identifier: TypeIdentifierNode,
	val start: Int,
	val end: Int,
	val operator: IRangeOperator
) : ITypeSliceNode {
	val lastIndex: Int = when (operator) {
		IRangeOperator.Inclusive -> end
		IRangeOperator.Exclusive -> end - 1
	}

	override fun getTypeName(): String = when (operator) {
		IRangeOperator.Inclusive -> "${identifier.getTypeName()}[$start...$end]"
		IRangeOperator.Exclusive -> "${identifier.getTypeName()}[$start..$end]"
	}

	override val value: String = getTypeName()
}

data class CollectionTypeLiteralNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val value: String,
	val typeExpressionNode: TypeExpressionNode
) : TypeExpressionNode {
	override fun getTypeName(): String {
		TODO("Not yet implemented")
	}
}