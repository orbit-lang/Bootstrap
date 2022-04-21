package org.orbit.core.nodes

import org.orbit.core.components.Token

abstract class TypeExpressionNode : LiteralNode<String>()

data class TypeIdentifierNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: String,
    val typeParametersNode: TypeParametersNode = TypeParametersNode(firstToken, lastToken)
) : TypeExpressionNode(), LValueTypeParameter {
	companion object {
		fun unit(token: Token) : TypeIdentifierNode
			= TypeIdentifierNode(token, token, "Orb::Types::Intrinsics::Unit")
	}

	override val name: TypeIdentifierNode = this

	val isWildcard: Boolean
		get() = value.endsWith("*")

	override fun equals(other: Any?): Boolean = when (other) {
		// TODO - Revisit equality when we get to implementing generics
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