package org.orbit.core.nodes

import org.orbit.core.components.Token

class LambdaLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val bindings: List<ParameterNode>,
    val body: BlockNode
) : ExpressionNode, ValueRepresentableNode, IInvokableDelegateNode {
    override fun getChildren(): List<INode>
        = bindings + body
}