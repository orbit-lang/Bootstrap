package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class RunNode(override val firstToken: Token, override val lastToken: Token, val prop: PropositionCallNode) : StatementNode() {
    override fun getChildren(): List<Node> = listOf(prop)
    override fun toString(): String = "run $prop"
}
