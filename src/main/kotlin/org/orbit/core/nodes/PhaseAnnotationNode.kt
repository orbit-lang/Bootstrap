package org.orbit.core.nodes

import org.orbit.core.components.Token

data class PhaseAnnotationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val annotationIdentifierNode: TypeIdentifierNode
) : Node() {

    override fun getChildren(): List<Node> {
        return listOf(annotationIdentifierNode)
    }
}