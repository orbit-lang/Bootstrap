package org.orbit.core.nodes

import org.orbit.core.Token
import org.orbit.frontend.rules.PhaseAnnotationNode

data class ObserverNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val phaseNode: PhaseAnnotationNode,
    val parameterNode: PairNode,
    val blockNode: BlockNode
) : TopLevelDeclarationNode(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(parameterNode, phaseNode, blockNode)
    }
}