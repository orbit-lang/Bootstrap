package org.orbit.core.nodes

import org.orbit.core.components.Token

data class CauseNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val invocationNode: EffectInvocationNode
) : IExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(invocationNode)
}

data class EffectInvocationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val effectIdentifier: TypeIdentifierNode,
    val args: List<IExpressionNode>
) : INode {
    override fun getChildren(): List<INode>
        = args + effectIdentifier
}