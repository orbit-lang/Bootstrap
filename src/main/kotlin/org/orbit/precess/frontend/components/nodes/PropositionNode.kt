package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.ast.NodeWalker
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.phase.Interpreter

data class PropositionNode(override val firstToken: Token, override val lastToken: Token, val propId: String, val enclosing: ContextLiteralNode, val body: PropositionStatementNode<*>) : StatementNode<PropositionNode>() {
    override fun getChildren(): List<Node> = listOf(enclosing, body)
    override fun toString(): String = "$propId = $enclosing => $body"

    override fun walk(interpreter: Interpreter): NodeWalker.WalkResult {
        return NodeWalker.WalkResult.Success { env ->
            val nInterpreter = interpreter.with(enclosing.contextId to env)
            val prop = body.getProposition(nInterpreter)

            interpreter.addProposition(propId) { prop.invoke(env) }

            env
        }
    }
}
