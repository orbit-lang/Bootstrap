package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.frontend.rules.IntrinsicContextCompositionOperator

interface ContextExpressionNode : INode

data class ContextNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val contextIdentifier: TypeIdentifierNode,
    val typeVariables: List<TypeIdentifierNode>,
    val clauses: List<WhereClauseNode>,
    val body: List<EntityDefNode>
) : INode {
    override fun getChildren(): List<INode>
        = listOf(contextIdentifier) + typeVariables + clauses + body
}

data class ContextInstantiationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val contextIdentifierNode: TypeIdentifierNode,
    val typeVariables: List<TypeExpressionNode>
) : ContextExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(contextIdentifierNode) + typeVariables
}

data class ContextCompositionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val op: IntrinsicContextCompositionOperator,
    val leftContext: ContextExpressionNode,
    val rightContext: ContextExpressionNode
) : ContextExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(leftContext, rightContext)
}