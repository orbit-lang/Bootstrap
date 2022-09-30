package org.orbit.core.nodes

import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.TokenTypes

sealed interface TypeExpressionNode : ILiteralNode<String>

data class InferNode(
	override val firstToken: Token,
	override val lastToken: Token
) : TypeExpressionNode {
	override val value: String = "_"
}

data class StructTypeNode(override val firstToken: Token, override val lastToken: Token, val members: List<PairNode>) : TypeExpressionNode {
	override val value: String = members.joinToString(", ")

	override fun getChildren(): List<INode> = members
}

data class TupleTypeNode(override val firstToken: Token, override val lastToken: Token, val left: TypeExpressionNode, val right: TypeExpressionNode) : TypeExpressionNode {
	override val value: String = "(${left.value}, ${right.value})"

	override fun getChildren(): List<INode>
		= listOf(left, right)
}

data class TypeIdentifierNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: String,
    val typeParametersNode: TypeParametersNode = TypeParametersNode(firstToken, lastToken)
) : TypeExpressionNode, LValueTypeParameter {
	companion object {
		private val nullToken = Token(TokenTypes.TypeIdentifier, "AnyType", SourcePosition.unknown)
		private val anyTypeIdentifierNode = TypeIdentifierNode(nullToken, nullToken, "AnyType")

		fun unit(token: Token) : TypeIdentifierNode
			= TypeIdentifierNode(token, token, "Orb::Types::Intrinsics::Unit")

		fun hole(token: Token) : TypeIdentifierNode
			= TypeIdentifierNode(token, token, "_")

		fun any(): TypeIdentifierNode
			= anyTypeIdentifierNode
	}

	override val name: TypeIdentifierNode = this

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

data class CollectionTypeLiteralNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val value: String,
	val typeExpressionNode: TypeExpressionNode
) : TypeExpressionNode