package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.frontend.rules.ContextCompositionOperator
import org.orbit.graph.pathresolvers.PathResolver

abstract class ContextExpressionNode : Node()

data class ContextNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val contextIdentifier: TypeIdentifierNode,
    val typeVariables: List<TypeIdentifierNode>,
    val clauses: List<WhereClauseNode>
) : Node() {
    override fun getChildren(): List<Node>
        = listOf(contextIdentifier) + typeVariables + clauses
}

data class ContextInstantiationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val contextIdentifierNode: TypeIdentifierNode,
    val typeVariables: List<TypeExpressionNode>
) : ContextExpressionNode() {
    override fun getChildren(): List<Node>
        = listOf(contextIdentifierNode) + typeVariables
}

data class ContextCompositionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val op: ContextCompositionOperator,
    val leftContext: ContextExpressionNode,
    val rightContext: ContextExpressionNode
) : ContextExpressionNode() {
    override fun getChildren(): List<Node>
        = listOf(leftContext, rightContext)
}