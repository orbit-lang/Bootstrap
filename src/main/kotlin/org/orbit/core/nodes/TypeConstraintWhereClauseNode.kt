package org.orbit.core.nodes

import org.orbit.core.components.Token

interface TypeConstraintNode : INode

data class TypeConstraintWhereClauseNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val statementNode: TypeConstraintNode
) : INode {
    override fun getChildren(): List<INode>
        = listOf(statementNode)
}