package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class CompoundPropositionCallNode(override val firstToken: Token, override val lastToken: Token, val props: List<PropositionCallNode>) : ExprNode() {
    override fun getChildren(): List<Node> = props
    override fun toString(): String = props.joinToString(" & ") { it.toString() }
}
