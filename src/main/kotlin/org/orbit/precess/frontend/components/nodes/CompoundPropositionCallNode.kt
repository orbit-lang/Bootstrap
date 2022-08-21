package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.phase.plus
import org.orbit.precess.backend.utils.AnyType

data class CompoundPropositionCallNode(override val firstToken: Token, override val lastToken: Token, val props: List<PropositionCallNode>) : PropositionStatementNode<CompoundPropositionCallNode>() {
    override fun getChildren(): List<Node> = props
    override fun toString(): String = props.joinToString(" & ") { it.toString() }

    override fun getProposition(interpreter: Interpreter): Proposition {
        val results = props.map { it.getProposition(interpreter) }

        return results.reduce(Proposition::plus)
    }
}
