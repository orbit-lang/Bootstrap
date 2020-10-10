package org.orbit.core.nodes

import org.orbit.core.Token

data class AssignmentStatementNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: IdentifierNode,
    val value: ExpressionNode
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return emptyList()
    }
}