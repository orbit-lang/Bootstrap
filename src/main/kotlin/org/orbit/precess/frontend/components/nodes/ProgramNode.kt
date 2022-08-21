package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.ast.NodeWalker
import org.orbit.precess.backend.phase.Interpreter

data class ProgramNode(override val firstToken: Token, override val lastToken: Token, val statements: List<StatementNode<*>>) : Node(), NodeWalker<ProgramNode> {
    override fun getChildren(): List<Node> = statements
    override fun toString(): String = statements.joinToString("\n") { it.toString() }

    override fun walk(interpreter: Interpreter): NodeWalker.WalkResult {
        return statements.map { it.walk(interpreter) }
            .reduce(NodeWalker.WalkResult::plus)
    }
}
