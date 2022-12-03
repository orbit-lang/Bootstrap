package org.orbit.core.nodes

import org.orbit.core.components.Token

interface IAttributeExpressionNode : INode

data class AttributeArrowNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val parameters: List<TypeIdentifierNode>,
    val constraint: IAttributeExpressionNode
) : INode {
    override fun getChildren(): List<INode>
        = parameters + constraint
}

data class AttributeOperatorExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val op: TypeBoundsOperator,
    val leftExpression: TypeExpressionNode,
    val rightExpression: TypeExpressionNode
) : IAttributeExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(leftExpression, rightExpression)
}

data class AttributeInvocationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: TypeIdentifierNode,
    val arguments: List<TypeExpressionNode>
) : IAttributeExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(identifier) + arguments
}

data class AttributeDefNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: TypeIdentifierNode,
    val arrow: AttributeArrowNode
) : TopLevelDeclarationNode {
    override val context: IContextExpressionNode? = null

    override fun getChildren(): List<INode>
        = listOf(identifier, arrow)
}
