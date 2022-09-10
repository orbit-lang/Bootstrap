package org.orbit.core.nodes

import org.orbit.core.components.Token

data class PanicNode(override val firstToken: Token, override val lastToken: Token, val expr: IExpressionNode) : IExpressionNode {
    override fun getChildren(): List<INode> = listOf(expr)
}
