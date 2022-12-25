package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeLambdaNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val domain: List<TypeExpressionNode>,
    val codomain: TypeExpressionNode,
    val constraints: List<TypeLambdaConstraintNode>,
    val elseClause: TypeExpressionNode? = null
) : TypeExpressionNode {
    override val value: String = "(${domain.joinToString(", ") { it.value }}) => ${codomain.value}"
    override fun getTypeName(): String = "TypeLambda"

    override fun getChildren(): List<INode> = when (elseClause) {
        null -> domain + codomain + constraints
        else -> domain + codomain + constraints + elseClause
    }
}

data class TypeLambdaConstraintNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val invocation: IAttributeExpressionNode
) : INode {
    override fun getChildren(): List<INode>
        = listOf(invocation)
}