package org.orbit.core.nodes

import org.orbit.core.components.Token

class TypeIdentifierWildcardNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val fullyQualifiedPart: TypeIdentifierNode
) : TypeExpressionNode() {
    override val value: String = fullyQualifiedPart.value

    override fun getChildren(): List<Node> = listOf(fullyQualifiedPart)
}