package org.orbit.core.nodes

import org.orbit.core.components.Token

data class AssignmentStatementNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: IdentifierNode,
    val value: IExpressionNode,
    val typeAnnotationNode: TypeExpressionNode? = null
) : WhereClauseExpressionNode, IMethodBodyStatementNode {
    override fun getChildren(): List<INode> = when (typeAnnotationNode) {
        null -> listOf(identifier, value)
        else -> listOf(identifier, value, typeAnnotationNode)
    }
}