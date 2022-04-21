package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeParametersNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeParameters: List<TypeIdentifierNode> = emptyList()
) : Node() {
	override fun getChildren() : List<Node> = typeParameters
}

abstract class AbstractTypeParameterNode : Node()

data class TypeParameterNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeIdentifierNode: TypeIdentifierNode
) : AbstractTypeParameterNode() {
    override fun getChildren(): List<Node> = listOf(typeIdentifierNode)
}

data class ConstrainedTypeParameterNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val constraints: List<TypeExpressionNode>
) : AbstractTypeParameterNode() {
    override fun getChildren(): List<Node> = constraints
}

interface LValueTypeParameter {
	val name: TypeIdentifierNode
}

data class BoundedTypeParameterNode(
    override val firstToken: Token,
    override val lastToken: Token,
	/// The name on the left of an optional `: Type` expression
    override val name: TypeIdentifierNode,
    val bound: RValueNode = RValueNode(TypeIdentifierNode(firstToken, firstToken, "Any"))
) : AbstractTypeParameterNode(), LValueTypeParameter {
	override fun getChildren() : List<Node> = listOf(name, bound)
	override fun toString() : String = "${name.value}: $bound"
}

data class DependentTypeParameterNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val name: TypeIdentifierNode,
    val type: RValueNode
) : AbstractTypeParameterNode(), LValueTypeParameter {
	override fun getChildren() : List<Node> = listOf(name, type)
	override fun toString() : String = "$name $type"
}

data class ValueTypeParameterNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val literalNode: RValueNode
) : AbstractTypeParameterNode() {
	override fun getChildren(): List<Node> = listOf(literalNode)
	override fun toString(): String = "RValue -> $literalNode"
}