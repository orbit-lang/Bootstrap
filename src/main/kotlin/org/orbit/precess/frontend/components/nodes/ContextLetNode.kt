package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.ast.NodeWalker
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.StatementWalker
import org.orbit.precess.backend.phase.Interpreter

abstract class StatementNode<Self: StatementNode<Self>> : Node(), StatementWalker<Self>

data class ContextLetNode(override val firstToken: Token, override val lastToken: Token, val nContext: ContextLiteralNode, val enclosing: ContextLiteralNode, val expr: ContextExprNode<*>) : StatementNode<ContextLetNode>() {
    override fun getChildren(): List<Node> = listOf(nContext, enclosing, expr)
    override fun toString(): String = "$nContext = $enclosing => $expr"

    override fun walk(interpreter: Interpreter): NodeWalker.WalkResult {
        return NodeWalker.WalkResult.Success { env ->
            val nInterpreter = interpreter.with(enclosing.contextId to env)
            val nEnv = expr.infer(nInterpreter, env) as Env

            interpreter.addContext(nContext.contextId) { nEnv }

            nEnv
        }
    }
}
