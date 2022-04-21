package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypes
import org.orbit.types.components.ConformanceBoundsConstraint
import org.orbit.types.components.EntityConstructorConstraint
import org.orbit.types.components.EqualityBoundsConstraint

abstract class WhereClauseExpressionNode : Node()

enum class TypeBoundsExpressionType(val op: TokenType, val entityConstructorConstraint: EntityConstructorConstraint) {
    Equals(TokenTypes.Assignment, EqualityBoundsConstraint), Conforms(TokenTypes.Colon, ConformanceBoundsConstraint);

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