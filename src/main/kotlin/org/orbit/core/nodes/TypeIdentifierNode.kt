package org.orbit.core.nodes

import org.orbit.core.components.Token
import java.io.Serializable

abstract class TypeExpressionNode(firstToken: Token, lastToken: Token, value: String)
	: LiteralNode<String>(firstToken, lastToken, value)

data class TypeIdentifierNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val value: String,
    val typeParametersNode: TypeParametersNode = TypeParametersNode(firstToken, lastToken)
) : TypeExpressionNode(firstToken, lastToken, value) {
	companion object {
		fun unit(token: Token) : TypeIdentifierNode
			= TypeIdentifierNode(token, token, "Unit")
	}

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