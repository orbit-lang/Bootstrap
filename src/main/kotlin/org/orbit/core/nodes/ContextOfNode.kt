package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ContextOfNode(override val firstToken: Token, override val lastToken: Token, val expressionNode: IExpressionNode) : IMethodBodyStatementNode, IContextExpressionNode {
    override fun getChildren(): List<INode> = listOf(expressionNode)
}
