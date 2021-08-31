package org.orbit.core.nodes

import org.orbit.core.components.Token

data class WhereClauseNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val whereStatement: Node
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(whereStatement)
    }
}