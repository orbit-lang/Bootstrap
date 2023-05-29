package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.frontend.rules.IntrinsicContextCompositionOperator

interface IContextExpressionNode : INode

sealed interface IContextDeclarationNode : INode
sealed interface IContextClauseExpressionNode : INode

data class ContextNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val contextIdentifier: TypeIdentifierNode,
    val typeVariables: List<IContextVariableNode>,
    val variables: List<PairNode>,
    val clauses: List<IContextClauseExpressionNode>,
    val body: List<IContextDeclarationNode>
) : INode {
    override fun getChildren(): List<INode>
        = listOf(contextIdentifier) + typeVariables + variables + clauses + body
}

data class ContextInstantiationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val contextIdentifierNode: TypeIdentifierNode,
    val typeParameters: List<TypeExpressionNode>,
    val valueParameters: List<IExpressionNode>
) : IContextExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(contextIdentifierNode) + typeParameters + valueParameters
}

data class ContextCompositionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val op: IntrinsicContextCompositionOperator,
    val leftContext: IContextExpressionNode,
    val rightContext: IContextExpressionNode
) : IContextExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(leftContext, rightContext)
}