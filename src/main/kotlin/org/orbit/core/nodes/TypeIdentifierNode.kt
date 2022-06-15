package org.orbit.core.nodes

import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.TokenTypes

abstract class TypeExpressionNode : LiteralNode<String>()

data class InferNode(
	override val firstToken: Token,
	override val lastToken: Token
) : TypeExpressionNode() {
	override val value: String = "_"
}

data class TypeIdentifierNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: String,
    val typeParametersNode: TypeParametersNode = TypeParametersNode(firstToken, lastToken)
) : TypeExpressionNode(), LValueTypeParameter {
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

	override fun equals(other: Any?): Boolean = when (other) {
		is TypeIdentifierNode -> value == other.value
		else -> false
	}

	override fun getChildren() : List<Node> {
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
) : TypeExpressionNode()