package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.ast.NodeWalker
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.phase.PropositionResult

data class PropositionCallNode(override val firstToken: Token, override val lastToken: Token, val propId: String, val context: ContextExprNode<*>) : PropositionStatementNode<PropositionCallNode>() {
    override fun getChildren(): List<Node> = listOf(context)
    override fun toString(): String = "$propId($context)"

    override fun getProposition(interpreter: Interpreter): Proposition {
        return { env ->
            when (val prop = interpreter.getProposition(propId)) {
                null -> { PropositionResult.False(IType.Never("Unknown Proposition $propId")) }
                else -> {
                    val nEnv = context.infer(interpreter, env) as Env

                    prop.invoke(nEnv)
                }
            }
        }
    }
}
