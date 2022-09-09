package org.orbit.core.nodes

import org.orbit.core.components.Token

data class CaseNode(override val firstToken: Token, override val lastToken: Token, val pattern: IPatternNode, val body: BlockNode) : INode {
    override fun getChildren(): List<INode> = listOf(pattern, body)
}
