package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TaggedTypeExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val tag: TypeIdentifierNode,
    val typeExpression: TypeExpressionNode
) : TypeExpressionNode {
    override val value: String = tag.value
    override fun getTypeName(): String = value

    override fun getChildren(): List<INode>
        = listOf(tag, typeExpression)
}

data class SumTypeNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val left: TaggedTypeExpressionNode,
    val right: TaggedTypeExpressionNode
) : TypeExpressionNode {
    override val value: String = "(${left.value} | ${right.value})"
    override fun getTypeName(): String = value

    override fun getChildren(): List<INode>
        = listOf(left, right)
}
