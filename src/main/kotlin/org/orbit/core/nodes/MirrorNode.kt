package org.orbit.core.nodes

import org.orbit.core.components.Token

data class MirrorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val expressionNode: IExpressionNode
) : TypeExpressionNode, IMethodBodyStatementNode {
    override val value: String = ""

    override fun getTypeName(): String {
        TODO("Not yet implemented")
    }

    override fun getChildren(): List<INode>
        = listOf(expressionNode)
}