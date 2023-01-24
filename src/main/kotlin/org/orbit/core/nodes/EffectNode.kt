package org.orbit.core.nodes

import org.orbit.core.components.Token

data class EffectNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: TypeIdentifierNode,
    val parameters: List<ParameterNode> = emptyList()
) : INode {
    override fun getChildren(): List<INode>
        = parameters + identifier
}
