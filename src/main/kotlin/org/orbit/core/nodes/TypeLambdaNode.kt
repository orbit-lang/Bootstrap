package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeLambdaNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val domain: List<TypeExpressionNode>,
    val codomain: TypeExpressionNode
) : TypeExpressionNode {
    override val value: String = "(${domain.joinToString(", ") { it.value }}) => ${codomain.value}"
    override fun getTypeName(): String = "TypeLambda"

    override fun getChildren(): List<INode>
        = domain + codomain
}
