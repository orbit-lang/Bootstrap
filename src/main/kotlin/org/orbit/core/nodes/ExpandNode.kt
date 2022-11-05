package org.orbit.core.nodes

import org.orbit.core.components.Token

interface IConstantExpressionNode : IExpressionNode {
    fun getTypeName() : String
}

data class ExpandNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val expressionNode: IConstantExpressionNode
) : TypeExpressionNode {
    override val value: String = expressionNode.getTypeName()
    override fun getTypeName(): String = expressionNode.getTypeName()
    override fun getChildren(): List<INode> = listOf(expressionNode)
}
