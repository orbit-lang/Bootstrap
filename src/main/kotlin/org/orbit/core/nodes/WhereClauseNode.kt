package org.orbit.core.nodes

import org.orbit.core.components.Token

interface WhereClauseExpressionNode : INode

data class WhereClauseTypeBoundsExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val boundsType: ITypeBoundsOperator,
    val sourceTypeExpression: TypeExpressionNode,
    val targetTypeExpression: TypeExpressionNode
) : WhereClauseExpressionNode {
    override fun getChildren(): List<INode> = listOf(sourceTypeExpression, targetTypeExpression)
}

data class WhereClauseNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val whereExpression: WhereClauseExpressionNode
) : INode {
    override fun getChildren(): List<INode> = listOf(whereExpression)
}