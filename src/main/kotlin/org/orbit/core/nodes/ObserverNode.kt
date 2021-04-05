package org.orbit.core.nodes

import org.orbit.core.Token

data class ObserverNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val observerIdentifierNode: MethodReferenceNode
) : TopLevelDeclarationNode(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(observerIdentifierNode)
    }
}