package org.orbit.core.nodes

import org.orbit.core.components.Token

interface InvokableNode : ExpressionNode {
    val parameterNodes: List<ExpressionNode>
}

data class ReferenceCallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val parameterNodes: List<ExpressionNode>,
    val referenceNode: ExpressionNode
) : InvokableNode, ValueRepresentableNode {
    override fun getChildren(): List<INode>
        = parameterNodes + referenceNode
}

data class MethodCallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val receiverExpression: ExpressionNode,
    val messageIdentifier: IdentifierNode,
    override val parameterNodes: List<ExpressionNode>,
    val isPropertyAccess: Boolean = false
) : InvokableNode, ValueRepresentableNode {
    override fun getChildren(): List<INode>
        = listOf(receiverExpression, messageIdentifier) + parameterNodes
}