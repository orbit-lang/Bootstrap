package org.orbit.core.nodes

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.Token
import org.orbit.core.components.TokenType
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

abstract class WhereClauseExpressionNode : Node()

sealed interface TypeBoundsOperator {
    companion object : KoinComponent {
        private val invocation: Invocation by inject()

        fun valueOf(token: Token) : TypeBoundsOperator = when {
            token.type == TokenTypes.Assignment -> Eq
            token.type == TokenTypes.Colon -> Like
            token.text == "^" -> KindEq
            token.type == TokenTypes.Identifier -> UserDefined(token.text)
            else -> throw invocation.make<Parser>("Illegal Constraint operator `${token.text}`", token)
        }
    }

    val op: String

    object Eq : TypeBoundsOperator { override val op: String = "=" }
    object Like : TypeBoundsOperator { override val op: String = ":" }
    object KindEq : TypeBoundsOperator { override val op: String = "^" }
    data class UserDefined(override val op: String) : TypeBoundsOperator
}

data class WhereClauseTypeBoundsExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val boundsType: TypeBoundsOperator,
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