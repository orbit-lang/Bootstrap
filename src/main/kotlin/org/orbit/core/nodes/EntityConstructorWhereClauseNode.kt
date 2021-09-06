package org.orbit.core.nodes

import org.orbit.core.components.Token

abstract class EntityConstructorWhereClauseStatementNode(
    override val firstToken: Token, override val lastToken: Token
) : Node(firstToken, lastToken)

data class EntityConstructorWhereClauseNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val statementNode: EntityConstructorWhereClauseStatementNode
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> = listOf(statementNode)
}