package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeParametersNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeParameters: List<TypeIdentifierNode> = emptyList()
) : INode {
	override fun getChildren() : List<INode>
	    = typeParameters
}

interface AbstractTypeParameterNode : INode

interface LValueTypeParameter {
	val name: TypeIdentifierNode
}

