package org.orbit.core.nodes

import org.orbit.core.components.Token

class MetaTypeNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeConstructorIdentifier: TypeIdentifierNode,
    val typeParameters: List<TypeIdentifierNode>
) : TypeExpressionNode(firstToken, lastToken, typeConstructorIdentifier.value) {
    override fun getChildren(): List<Node> {
        return listOf(typeConstructorIdentifier) + typeParameters
    }
}