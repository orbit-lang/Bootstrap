package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeLambdaInvocationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeIdentifierNode: TypeIdentifierNode,
    val arguments: List<TypeExpressionNode>
) : TypeExpressionNode {
    override val value: String = typeIdentifierNode.value
    override fun getTypeName(): String = ""

    override fun getChildren(): List<INode>
        = listOf(typeIdentifierNode) + arguments
}
