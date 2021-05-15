package org.orbit.core.nodes

import org.orbit.core.Token
import org.orbit.graph.PathResolver

data class ObserverNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val observerIdentifierNode: MethodReferenceNode
) : TopLevelDeclarationNode(firstToken, lastToken, PathResolver.Pass.Initial) {
    override fun getChildren(): List<Node> {
        return listOf(observerIdentifierNode)
    }
}