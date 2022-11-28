package org.orbit.core.nodes

import org.orbit.core.components.Token

data class LambdaTypeNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val domain: List<TypeExpressionNode>,
    val codomain: TypeExpressionNode
) : TypeExpressionNode {
    override val value: String = "->"
    override fun getTypeName(): String {
        TODO("Not yet implemented")
    }

    override fun getChildren(): List<INode> = domain + codomain
}
