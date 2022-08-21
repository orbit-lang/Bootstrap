package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.utils.AnyType

data class ContextCallNode(override val firstToken: Token, override val lastToken: Token, val outer: ContextLiteralNode, val inner: ContextLiteralNode) : ContextExprNode<ContextCallNode>() {
    override fun getChildren(): List<Node> = listOf(outer, inner)
    override fun toString(): String = "$outer($inner)"

    override fun infer(interpreter: Interpreter, env: Env): AnyType {
        val outer = interpreter.getContext(outer.contextId)
            ?: return IType.Never("Unknown Context $outer")

        val inner = interpreter.getContext(inner.contextId)
            ?: return IType.Never("Unknown Context $outer")

        return outer(inner(env))
    }
}
