package org.orbit.core.nodes

import org.orbit.core.components.Token
import java.io.Serializable

data class ConstructorInvocationNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeExpressionNode: TypeExpressionNode,
    val parameterNodes: List<IExpressionNode>
) : IExpressionNode, Serializable, ValueRepresentableNode, IConstantExpressionNode {
    override fun getChildren(): List<INode> {
        return listOf(typeExpressionNode) + parameterNodes
    }

    override fun getTypeName(): String = typeExpressionNode.getTypeName()
}