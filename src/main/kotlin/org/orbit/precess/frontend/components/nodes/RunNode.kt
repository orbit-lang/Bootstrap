package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.ast.NodeWalker
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.PropositionResult

data class RunNode(override val firstToken: Token, override val lastToken: Token, val prop: PropositionCallNode) : StatementNode<RunNode>() {
    override fun getChildren(): List<Node> = listOf(prop)
    override fun toString(): String = "run $prop"

    override fun walk(interpreter: Interpreter): NodeWalker.WalkResult = NodeWalker.WalkResult.Success { env ->
        prop.getProposition(interpreter).invoke(env).invoke()
    }
}
