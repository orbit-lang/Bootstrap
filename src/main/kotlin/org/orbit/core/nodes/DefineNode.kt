package org.orbit.core.nodes

import org.orbit.core.components.Token

data class DefineNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val keySymbolNode: SymbolLiteralNode,
    val outputSymbolNode: SymbolLiteralNode
) : INode {
    override fun getChildren(): List<INode> {
        return listOf(keySymbolNode, outputSymbolNode)
    }
}