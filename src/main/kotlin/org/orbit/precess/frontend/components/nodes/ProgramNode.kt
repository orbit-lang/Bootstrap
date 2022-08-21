package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class ProgramNode(override val firstToken: Token, override val lastToken: Token, val statements: List<StatementNode>) : Node() {
    override fun getChildren(): List<Node> = statements
    override fun toString(): String = statements.joinToString("\n") { it.toString() }
}
