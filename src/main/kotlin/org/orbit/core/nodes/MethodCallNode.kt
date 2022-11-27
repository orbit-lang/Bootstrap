package org.orbit.core.nodes

import org.orbit.core.components.Token

interface IInvokableNode : IExpressionNode {
    val arguments: List<IExpressionNode>
}

data class ReferenceCallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val arguments: List<IExpressionNode>,
    val referenceNode: IExpressionNode
) : IInvokableNode, ValueRepresentableNode, IPatternNode {
    override fun getChildren(): List<INode>
        = arguments + referenceNode
}

data class MethodCallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val receiverExpression: IExpressionNode,
    val messageIdentifier: IdentifierNode,
    override val arguments: List<IExpressionNode>,
    val isPropertyAccess: Boolean = false
) : IInvokableNode, ValueRepresentableNode, IPatternNode, IMethodBodyStatementNode, IConstantExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(receiverExpression, messageIdentifier) + arguments

    override fun getTypeName(): String = ""
}