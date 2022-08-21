package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.ast.NodeWalker
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.utils.AnyType

data class WeakenNode<D>(override val firstToken: Token, override val lastToken: Token, val context: ContextLiteralNode, val decl: D) : ContextExprNode<WeakenNode<D>>() where D : DeclNode<D> {
    override fun getChildren(): List<Node> = listOf(context, decl)
    override fun toString(): String = "($context + $decl)"

    override fun infer(interpreter: Interpreter, env: Env): AnyType {
        val pEnv = context.infer(interpreter, env) as Env
        val nEnv = decl.walk(interpreter)

        return nEnv(pEnv)
    }
}
