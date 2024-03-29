package org.orbit.core.nodes

import org.orbit.core.components.Token

data class MethodReferenceNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val isConstructor: Boolean,
    val typeExpressionNode: TypeExpressionNode,
    val identifierNode: IdentifierNode
) : IExpressionNode, IDelegateNode, IInvokableDelegateNode {
    override fun getChildren(): List<INode>
        = listOf(typeExpressionNode, identifierNode)
}

fun MethodSignatureNode.toMethodReferenceNode() : MethodReferenceNode
    = MethodReferenceNode(firstToken, lastToken, false, receiverTypeNode, identifierNode)