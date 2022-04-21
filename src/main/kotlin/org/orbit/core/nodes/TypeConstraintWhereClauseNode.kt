package org.orbit.core.nodes

import org.orbit.core.components.Token

abstract class TypeConstraintNode : Node()

data class TypeConstraintWhereClauseNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val statementNode: TypeConstraintNode
) : Node() {
    override fun getChildren(): List<Node> = listOf(statementNode)
}