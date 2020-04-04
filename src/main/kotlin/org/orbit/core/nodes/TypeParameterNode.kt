package org.orbit.core.nodes

import org.orbit.core.Token

data class TypeParametersNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val typeParameterNodes: List<TypeParameterNode> = emptyList()
) : Node(firstToken, lastToken) {
	override fun getChildren() : List<Node> = typeParameterNodes
}

abstract class TypeParameterNode(
	override val firstToken: Token,
	override val lastToken: Token,
	open val name: TypeIdentifierNode
) : Node(firstToken, lastToken)

data class BoundedTypeParameterNode(
	override val firstToken: Token,
	override val lastToken: Token,
	/// The name on the left of an optional `: Type` expression
	override val name: TypeIdentifierNode,
	val bound: TypeIdentifierNode = TypeIdentifierNode(firstToken, firstToken, "Any")
) : TypeParameterNode(firstToken, lastToken, name) {
	override fun getChildren() : List<Node>
		= listOf(name, bound)

	override fun toString() : String
		= "${name.typeIdentifier}: ${bound.typeIdentifier}"
}

data class DependentTypeParameterNode(
	override val firstToken: Token,
	override val lastToken: Token,
	override val name: TypeIdentifierNode,
	val type: TypeIdentifierNode
) : TypeParameterNode(firstToken, lastToken, name) {
	override fun getChildren() : List<Node>
		= listOf(name, type)

	override fun toString() : String
		= "${name} ${type}"
}