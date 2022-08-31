package org.orbit.core.nodes

import org.orbit.core.components.Token

class MetaTypeNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeConstructorIdentifier: TypeIdentifierNode,
    val typeParameters: List<TypeExpressionNode>
) : TypeExpressionNode {
    override val value: String = typeConstructorIdentifier.value

    override fun getChildren(): List<INode> {
        return listOf(typeConstructorIdentifier) + typeParameters
    }
}