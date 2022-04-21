package org.orbit.core.nodes

import org.orbit.core.components.Token

data class MacroScopeNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val defineNodes: List<DefineNode>
) : Node() {
    override fun getChildren(): List<Node> = defineNodes
}