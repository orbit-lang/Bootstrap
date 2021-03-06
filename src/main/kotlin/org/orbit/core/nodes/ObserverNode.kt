package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.graph.pathresolvers.PathResolver

data class ObserverNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val observerIdentifierNode: MethodReferenceNode
) : TopLevelDeclarationNode(firstToken, lastToken, PathResolver.Pass.Initial) {
    override fun getChildren(): List<Node> {
        return listOf(observerIdentifierNode)
    }
}