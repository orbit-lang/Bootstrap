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
    val isPropertyAccess: Boolean = false,
    val effectHandler: EffectHandlerNode? = null
) : IInvokableNode, ValueRepresentableNode, IPatternNode, IMethodBodyStatementNode, IConstantExpressionNode {
    override fun getChildren(): List<INode> = when (effectHandler) {
        null -> listOf(receiverExpression, messageIdentifier) + arguments
        else -> listOf(receiverExpression, messageIdentifier) + arguments + effectHandler
    }

    fun withEffectHandler(effectHandler: EffectHandlerNode) : MethodCallNode
        = MethodCallNode(firstToken, lastToken, receiverExpression, messageIdentifier, arguments, isPropertyAccess, effectHandler)

    override fun getTypeName(): String = ""
}