package org.orbit.core.nodes

import org.orbit.core.components.Token

abstract class TypeConstraintNode(
    override val firstToken: Token, override val lastToken: Token
) : Node(firstToken, lastToken)

data class TypeConstraintWhereClauseNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val statementNode: TypeConstraintNode
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> = listOf(statementNode)
}