package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeIndexNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val index: TypeIdentifierNode
) : TypeExpressionNode(firstToken, lastToken, "Self") {
    override fun getChildren(): List<Node> = listOf(index)
}