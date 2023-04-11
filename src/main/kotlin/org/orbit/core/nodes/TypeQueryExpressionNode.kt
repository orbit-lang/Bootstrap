package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TypeQueryExpressionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val resultIdentifier: TypeIdentifierNode,
    val clause: IAttributeExpressionNode
) : TypeExpressionNode {
    override fun getTypeName(): String
        = resultIdentifier.getTypeName()

    override val value: String = getTypeName()

    override fun getChildren(): List<INode>
        = listOf(resultIdentifier, clause)
}
