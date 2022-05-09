package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypes

abstract class WhereClauseExpressionNode : Node()

enum class TypeBoundsExpressionType(val op: TokenType) {
    Equals(TokenTypes.Assignment), Conforms(TokenTypes.Colon);

    companion object {
        fun valueOf(op: TokenType) : TypeBoundsExpressionType?
            = values().firstOrNull { it.op == op }

        fun allOps() : Array<TokenType>
            = values().map { it.op }.toTypedArray()
    }
}

data class WhereClauseTypeBoundsExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val boundsType: TypeBoundsExpressionType,
    val sourceTypeExpression: TypeExpressionNode,
    val targetTypeExpression: TypeExpressionNode
) : WhereClauseExpressionNode() {
    override fun getChildren(): List<Node> = listOf(sourceTypeExpression, targetTypeExpression)
}

data class WhereClauseNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val whereExpression: WhereClauseExpressionNode
) : Node() {
    override fun getChildren(): List<Node> = listOf(whereExpression)
}