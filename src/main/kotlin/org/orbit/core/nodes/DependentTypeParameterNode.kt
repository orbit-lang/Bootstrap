package org.orbit.core.nodes

import org.orbit.core.components.Token

data class DependentTypeParameterNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: TypeIdentifierNode,
    val type: TypeExpressionNode
) : ITypeLambdaParameterNode {
    override fun getTypeName(): String
        = "$identifier $type"

    override val value: String = getTypeName()

	override fun getChildren() : List<INode> = listOf(identifier, type)
}