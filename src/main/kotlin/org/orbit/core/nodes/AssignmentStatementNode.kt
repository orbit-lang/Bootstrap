package org.orbit.core.nodes

import org.orbit.core.components.Token

data class AssignmentStatementNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: IdentifierNode,
    val value: ExpressionNode
) : WhereClauseExpressionNode(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return emptyList()
    }
}