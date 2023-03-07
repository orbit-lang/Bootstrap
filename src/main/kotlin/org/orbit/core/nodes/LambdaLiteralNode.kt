package org.orbit.core.nodes

import org.orbit.core.components.Token

data class LambdaBindingsNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val bindings: List<ParameterNode>
) : INode {
    override fun getChildren(): List<INode> = bindings
}

data class LambdaLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val bindings: List<ParameterNode>,
    val body: BlockNode
) : IExpressionNode, ValueRepresentableNode, IInvokableDelegateNode, IDelegateNode {
    override fun getChildren(): List<INode>
        = bindings + body
}

data class InvocationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val invokable: IExpressionNode,
    override val arguments: List<IExpressionNode>,
    val effectHandler: EffectHandlerNode?
) : IExpressionNode, IInvokableNode {
    override fun getChildren(): List<INode> = listOf(invokable) + arguments
}