package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.utils.AnyType

data class ContextLiteralNode(override val firstToken: Token, override val lastToken: Token, val contextId: String) : ContextExprNode<ContextLiteralNode>() {
    override fun getChildren(): List<Node> = emptyList()
    override fun toString(): String = contextId

    override fun infer(interpreter: Interpreter, env: Env): AnyType {
        val nEnv = interpreter.getContext(contextId)
            ?: return IType.Never("Unknown Context $contextId")

        return nEnv(env)
    }
}