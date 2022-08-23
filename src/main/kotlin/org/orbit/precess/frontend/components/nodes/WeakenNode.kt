package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.phase.PropositionResult
import org.orbit.precess.backend.utils.AnyType

data class WeakenNode(override val firstToken: Token, override val lastToken: Token, val context: ContextLiteralNode, val decl: DeclNode<*>) : PropositionExpressionNode() {
    override fun getChildren(): List<Node> = listOf(context, decl)
    override fun toString(): String = "$context + $decl"

    override fun getProposition(interpreter: Interpreter): Proposition = { env ->
        PropositionResult.True(env.extend(decl.getDecl(env)))
    }
}
