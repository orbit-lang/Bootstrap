package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.utils.AnyType

data class CompoundContextCallNode(override val firstToken: Token, override val lastToken: Token, val calls: List<ContextCallNode>) : ContextExprNode<CompoundContextCallNode>() {
    override fun getChildren(): List<Node> = calls
    override fun toString(): String = calls.joinToString(" + ") { it.toString() }

    override fun infer(interpreter: Interpreter, env: Env): AnyType {
        val envs = calls.map { it.infer(interpreter, env) as Env }

        return envs.reduce { acc, next -> acc + next }
    }
}
