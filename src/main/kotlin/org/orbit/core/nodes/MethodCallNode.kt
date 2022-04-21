package org.orbit.core.nodes

import org.orbit.core.components.Token

abstract class InvokableNode(
    override val firstToken: Token,
    override val lastToken: Token,
    open val parameterNodes: List<ExpressionNode>
) : ExpressionNode()

data class ReferenceCallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val parameterNodes: List<ExpressionNode>,
    val referenceNode: ExpressionNode
) : InvokableNode(firstToken, lastToken, parameterNodes), ValueRepresentableNode {
    override fun getChildren(): List<Node>
        = parameterNodes + referenceNode
}

data class MethodCallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val receiverExpression: ExpressionNode,
    val messageIdentifier: IdentifierNode,
    override val parameterNodes: List<ExpressionNode>,
    val isPropertyAccess: Boolean = false
) : InvokableNode(firstToken, lastToken, parameterNodes), ValueRepresentableNode {
    val isInstanceCall: Boolean
        get() = when (receiverExpression) {
            is RValueNode -> receiverExpression.expressionNode !is TypeExpressionNode
            else -> true
        }

    override fun getChildren(): List<Node> {
        return listOf(receiverExpression, messageIdentifier) + parameterNodes
    }
}