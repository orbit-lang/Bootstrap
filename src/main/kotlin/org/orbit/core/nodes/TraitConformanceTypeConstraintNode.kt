package org.orbit.core.nodes

import org.orbit.core.components.Token

data class TraitConformanceTypeConstraintNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val constrainedTypeNode: TypeIdentifierNode,
    val constraintTraitNode: TypeExpressionNode
) : TypeConstraintNode() {
    override fun getChildren(): List<Node>
        = listOf(constrainedTypeNode, constraintTraitNode)
}