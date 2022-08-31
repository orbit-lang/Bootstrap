package org.orbit.core.nodes

import org.orbit.core.components.Token
import java.io.Serializable

data class ConstructorNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val typeExpressionNode: TypeExpressionNode,
    val parameterNodes: List<ExpressionNode>
) : ExpressionNode, Serializable, ValueRepresentableNode {
    override fun getChildren(): List<INode> {
        return listOf(typeExpressionNode) + parameterNodes
    }
}