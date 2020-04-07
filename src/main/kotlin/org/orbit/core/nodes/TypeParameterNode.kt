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
	override val lastToken: Token
) : Node(firstToken, lastToken)

data class BoundedTypeParameterNode(
	override val firstToken: Token,
	override val lastToken: Token,
	/// The name on the left of an optional `: Type` expression
	val name: TypeIdentifierNode,
	val bound: RValueNode = RValueNode(TypeIdentifierNode(firstToken, firstToken, "Any"))
) : TypeParameterNode(firstToken, lastToken) {
	override fun getChildren() : List<Node> = listOf(name, bound)
	override fun toString() : String = "${name.value}: $bound"
}

data class DependentTypeParameterNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val name: TypeIdentifierNode,
	val type: RValueNode
) : TypeParameterNode(firstToken, lastToken) {
	override fun getChildren() : List<Node> = listOf(name, type)
	override fun toString() : String = "$name $type"
}

data class ValueTypeParameterNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val literalNode: RValueNode
) : TypeParameterNode(firstToken, lastToken) {
	override fun getChildren(): List<Node> = listOf(literalNode)
	override fun toString(): String = "RValue -> $literalNode"
}