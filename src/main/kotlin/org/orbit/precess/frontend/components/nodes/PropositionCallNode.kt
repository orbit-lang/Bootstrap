package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.phase.PropositionResult

data class PropositionCallNode(override val firstToken: Token, override val lastToken: Token, val propId: String, val arg: PropositionExpressionNode) : PropositionExpressionNode() {
    override fun getChildren(): List<Node> = listOf(arg)
    override fun toString(): String = "$propId($arg)"

    override fun getProposition(interpreter: Interpreter): Proposition = {
        val prop = interpreter.getProposition(propId)
            ?: { PropositionResult.False(IType.Never("Unknown Proposition: `$propId` in $interpreter")) }

        val ctx = arg.getProposition(interpreter).invoke(it)

        when (ctx) {
            is PropositionResult.True -> prop(ctx.env)
            is PropositionResult.False -> ctx
        }
    }
}
