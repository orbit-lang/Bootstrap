package org.orbit.core.nodes

import org.orbit.core.components.Token

data class WhereClauseByExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifierNode: IdentifierNode,
    val lambdaExpression: LambdaLiteralNode
) : WhereClauseExpressionNode() {
    override fun getChildren(): List<Node> = listOf(identifierNode, lambdaExpression)
}
