package org.orbit.core.nodes

import org.orbit.core.components.Token

data class EffectHandlerNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val flowIdentifier: IdentifierNode,
    val cases: List<CaseNode>
) : IInvokableDelegateNode {
    override fun getChildren(): List<INode>
        = cases + flowIdentifier
}
