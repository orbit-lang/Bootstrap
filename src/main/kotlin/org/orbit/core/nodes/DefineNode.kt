package org.orbit.core.nodes

import org.orbit.core.Token
import org.orbit.graph.pathresolvers.PathResolver

data class DefineNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val keySymbolNode: SymbolLiteralNode,
    val outputSymbolNode: SymbolLiteralNode
) : TopLevelDeclarationNode(firstToken, lastToken, PathResolver.Pass.Initial) {
    override fun getChildren(): List<Node> {
        return listOf(keySymbolNode, outputSymbolNode)
    }
}