package org.orbit.core.nodes

import org.orbit.core.components.Token

data class SelectNode(override val firstToken: Token, override val lastToken: Token, val condition: IExpressionNode, val binding: IdentifierNode?, val cases: List<CaseNode>, val inSingleExpressionPosition: Boolean = false) : IExpressionNode {
    override fun getChildren(): List<INode> = when (binding) {
        null -> listOf(condition) + cases
        else -> listOf(condition, binding) + cases
    }
}
