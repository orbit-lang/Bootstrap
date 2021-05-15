package org.orbit.core.nodes

import org.orbit.core.Token

data class CallNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val receiverExpression: ExpressionNode,
    val messageIdentifier: IdentifierNode,
    val parameterNodes: List<ExpressionNode>,
    val isPropertyAccess: Boolean = false
    // TODO - Parameters
) : ExpressionNode(firstToken, lastToken) {
    override fun getChildren(): List<Node> {
        return listOf(receiverExpression, messageIdentifier) + parameterNodes
    }
}
//
//data class InstanceMethodCallNode(
//    override val firstToken: Token,
//    override val lastToken: Token,
//    override val methodIdentifierNode: IdentifierNode,
//    val receiverNode: IdentifierNode,
//    override val parameterNodes: List<ExpressionNode>
//) : MethodCallNode(firstToken, lastToken, methodIdentifierNode, parameterNodes) {
//    override fun getChildren(): List<Node> {
//        return listOf(methodIdentifierNode, receiverNode) + parameterNodes
//    }
//}
//
//data class TypeMethodCallNode(
//    override val firstToken: Token,
//    override val lastToken: Token,
//    override val methodIdentifierNode: IdentifierNode,
//    override val parameterNodes: List<ExpressionNode>,
//    val receiverTypeNode: TypeIdentifierNode
//) : MethodCallNode(firstToken, lastToken, methodIdentifierNode, parameterNodes) {
//    override fun getChildren(): List<Node> {
//        return listOf(methodIdentifierNode, receiverTypeNode) + parameterNodes
//    }
//}