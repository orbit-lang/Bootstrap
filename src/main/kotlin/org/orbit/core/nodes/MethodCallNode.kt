package org.orbit.core.nodes

import org.orbit.core.components.Token

interface InvokableNode : IExpressionNode {
    val parameterNodes: List<IExpressionNode>
}

data class ReferenceCallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val parameterNodes: List<IExpressionNode>,
    val referenceNode: IExpressionNode
) : InvokableNode, ValueRepresentableNode, IPatternNode {
    override fun getChildren(): List<INode>
        = parameterNodes + referenceNode
}

data class MethodCallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val receiverExpression: IExpressionNode,
    val messageIdentifier: IdentifierNode,
    override val parameterNodes: List<IExpressionNode>,
    val isPropertyAccess: Boolean = false
) : InvokableNode, ValueRepresentableNode, IPatternNode, IMethodBodyStatementNode, IConstantExpressionNode {
    override fun getChildren(): List<INode>
        = listOf(receiverExpression, messageIdentifier) + parameterNodes

    override fun getTypeName(): String = ""
}