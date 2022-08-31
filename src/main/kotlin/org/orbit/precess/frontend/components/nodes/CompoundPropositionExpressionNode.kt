package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.phase.plus

data class CompoundPropositionExpressionNode(override val firstToken: Token, override val lastToken: Token, val props: List<PropositionExpressionNode>) : PropositionExpressionNode {
    override fun getChildren(): List<INode> = props
    override fun toString(): String = props.joinToString(" => ")

    override fun getProposition(interpreter: Interpreter): Proposition
        = props.map { it.getProposition(interpreter) }.reduce(Proposition::plus)
}
