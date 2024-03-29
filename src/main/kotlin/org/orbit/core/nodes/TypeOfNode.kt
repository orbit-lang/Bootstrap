package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeOfNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val expressionNode: IExpressionNode
) : IMethodBodyStatementNode {
    override fun getChildren(): List<INode>
        = listOf(expressionNode)
}
