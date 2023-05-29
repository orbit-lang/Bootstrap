package org.orbit.core.nodes

import org.orbit.core.components.Token

sealed interface ITypeConstraintNode : TypeExpressionNode

sealed interface IContextVariableNode : TypeExpressionNode

data class ConstrainedTypeVarNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val identifier: TypeIdentifierNode,
    val constraint: ITypeConstraintNode
) : IContextVariableNode {
    override val value: String = "${identifier.value} : ${constraint.value}"

    override fun getTypeName(): String
        = value

    override fun getChildren(): List<INode>
        = listOf(identifier, constraint)
}
