package org.orbit.core.nodes

import org.orbit.core.Token

abstract class MethodCallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    open val methodIdentifierNode: IdentifierNode,
    open val parameterNodes: List<ExpressionNode>
    // TODO - Parameters
) : ExpressionNode(firstToken, lastToken)

data class InstanceMethodCallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    override val methodIdentifierNode: IdentifierNode,
    val receiverNode: IdentifierNode,
    override val parameterNodes: List<ExpressionNode>
) : MethodCallNode(firstToken, lastToken, methodIdentifierNode, parameterNodes) {
    override fun getChildren(): List<Node> {
        return listOf(methodIdentifierNode, receiverNode) + parameterNodes
    }
}