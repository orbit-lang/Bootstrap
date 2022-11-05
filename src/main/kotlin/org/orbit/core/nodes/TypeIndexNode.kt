package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeIndexNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val index: TypeIdentifierNode
) : TypeExpressionNode {
    override val value: String = "Self"

    override fun getChildren(): List<INode>
        = listOf(index)

    override fun getTypeName(): String {
        TODO("Not yet implemented")
    }
}