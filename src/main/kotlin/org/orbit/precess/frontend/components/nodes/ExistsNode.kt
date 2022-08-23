package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.phase.PropositionResult

data class ExistsNode(override val firstToken: Token, override val lastToken: Token, val decl: DeclNode<*>, val expr: PropositionExpressionNode) : PropositionExpressionNode() {
    override fun getChildren(): List<Node> = listOf(decl)
    override fun toString(): String = "exists $decl in $expr"

    override fun getProposition(interpreter: Interpreter): Proposition = { env ->
        when (val d = decl.getDecl(env)) {
            is DeclResult.Success -> {
                val nEnv = env.extend(d.decl)

                expr.getProposition(interpreter).invoke(nEnv)
            }

            is DeclResult.Failure -> PropositionResult.False(d.reason)
        }
    }
}
