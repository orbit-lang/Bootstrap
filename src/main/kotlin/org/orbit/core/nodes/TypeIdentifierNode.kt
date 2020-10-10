package org.orbit.core.nodes

import org.orbit.core.Token

data class TypeIdentifierNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val value: String,
	val typeParametersNode: TypeParametersNode = TypeParametersNode(firstToken, lastToken)
) : LiteralNode<String>(firstToken, lastToken, value) {
	companion object {
		fun unit(token: Token) : TypeIdentifierNode
			= TypeIdentifierNode(token, token, "Unit")
	}

	override fun getChildren() : List<Node> {
		return typeParametersNode.typeParameters
	}

	override fun toString() : String
		= "${value}<${typeParametersNode.typeParameters.joinToString(", ") { it.toString() }}>"
}