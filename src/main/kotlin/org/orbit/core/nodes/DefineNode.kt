package org.orbit.core.nodes

import org.orbit.core.Token

data class DefineNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val keySymbolNode: SymbolLiteralNode,
    val outputSymbolNode: SymbolLiteralNode
) : TopLevelDeclarationNode(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(keySymbolNode, outputSymbolNode)
    }
}