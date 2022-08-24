package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.phase.PropositionResult

data class DumpNode(override val firstToken: Token, override val lastToken: Token, private val tag: RefLiteralNode? = null) : PropositionExpressionNode() {
    override fun getChildren(): List<Node> = emptyList()
    override fun toString(): String = "dump(∆)"

    override fun getProposition(interpreter: Interpreter): Proposition = { env ->
        interpreter.takeSnapshot(env, tag?.refId)

        PropositionResult.True(env)
    }
}
