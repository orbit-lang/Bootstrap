package org.orbit.core.nodes

import org.orbit.core.components.Token

data class CheckNode(override val firstToken: Token, override val lastToken: Token, val left: IExpressionNode, val right: IExpressionNode) : INode {
    override fun getChildren(): List<INode>
        = listOf(left, right)
}
