package org.orbit.core.nodes

import org.orbit.core.components.IIntrinsicOperator
import org.orbit.core.components.Token

enum class CaseMode(override val symbol: String): IIntrinsicOperator {
    Direct("="), Indirect("by");

    companion object : IIntrinsicOperator.Factory<CaseMode> {
        override fun all(): List<CaseMode> = values().toList()
    }
}

data class CaseNode(override val firstToken: Token, override val lastToken: Token, val pattern: IPatternNode, val mode: CaseMode, val body: IExpressionNode) : INode {
    override fun getChildren(): List<INode> = listOf(pattern, body)
}
