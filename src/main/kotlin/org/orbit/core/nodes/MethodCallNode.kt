package org.orbit.core.nodes

import org.orbit.core.components.Token

data class CallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val receiverExpression: ExpressionNode,
    val messageIdentifier: IdentifierNode,
    val parameterNodes: List<ExpressionNode>,
    val isPropertyAccess: Boolean = false
) : ExpressionNode(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(receiverExpression, messageIdentifier) + parameterNodes
    }
}