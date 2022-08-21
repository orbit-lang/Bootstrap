package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.utils.AnyType

data class ArrowNode(override val firstToken: Token, override val lastToken: Token, val domain: TypeExprNode, val codomain: TypeExprNode) : TypeExprNode() {
    override fun getChildren(): List<Node> = listOf(domain, codomain)
    override fun toString(): String = "($domain) -> $codomain"

    override fun infer(interpreter: Interpreter, env: Env): AnyType {
        val dType = domain.infer(interpreter, env)
        val cType = codomain.infer(interpreter, env)

        return IType.Arrow1(dType, cType)
    }
}
