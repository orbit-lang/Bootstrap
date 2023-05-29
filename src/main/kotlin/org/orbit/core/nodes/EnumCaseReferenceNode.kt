package org.orbit.core.nodes

import org.orbit.core.components.Token

data class EnumCaseReferenceNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val case: IdentifierNode
) : IConstantExpressionNode, IPatternNode {
    override fun getTypeName(): String = ""

    override fun getChildren(): List<INode>
        = listOf(case)
}
