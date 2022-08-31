package org.orbit.core.nodes

import org.orbit.core.components.Token

data class MacroScopeNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val defineNodes: List<DefineNode>
) : INode {
    override fun getChildren(): List<INode> = defineNodes
}