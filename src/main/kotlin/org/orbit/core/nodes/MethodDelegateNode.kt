package org.orbit.core.nodes

import org.orbit.core.components.Token

data class MethodDelegateNode(override val firstToken: Token, override val lastToken: Token, val methodName: IdentifierNode, val delegate: IDelegateNode) : IProjectionDeclarationNode, IWithStatementNode, ISignatureRepresentableNode {
    override fun getChildren(): List<INode> = listOf(methodName, delegate)
}

fun MethodSignatureNode.toMethodDelegateNode() : MethodDelegateNode
    = MethodDelegateNode(firstToken, lastToken, identifierNode, toMethodReferenceNode())
