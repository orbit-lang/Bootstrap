package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.ContextOperator
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.phase.PropositionResult
import org.orbit.precess.backend.utils.AnyType

data class ModifyContextNode(override val firstToken: Token, override val lastToken: Token, val context: ContextLiteralNode, val decl: DeclNode<*>, val contextOperator: ContextOperator) : PropositionExpressionNode() {
    override fun getChildren(): List<Node> = listOf(context, decl)
    override fun toString(): String = "$context + $decl"

    override fun getProposition(interpreter: Interpreter): Proposition = { env ->
        when (val d = decl.getDecl(env)) {
            is DeclResult.Success -> PropositionResult.True(when (contextOperator) {
                ContextOperator.Extend -> env.extend(d.decl)
                ContextOperator.Reduce -> env.reduce(d.decl)
            })
            is DeclResult.Failure -> PropositionResult.False(d.reason)
        }
    }
}
