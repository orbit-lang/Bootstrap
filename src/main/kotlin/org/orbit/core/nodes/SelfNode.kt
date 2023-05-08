package org.orbit.core.nodes

import org.orbit.core.components.Token

data class SelfNode(
    override val firstToken: Token,
    override val lastToken: Token
) : TypeExpressionNode {
    override fun getTypeName(): String = "Self"
    override val value: String = getTypeName()

    override fun getChildren(): List<INode>
        = emptyList()
}
