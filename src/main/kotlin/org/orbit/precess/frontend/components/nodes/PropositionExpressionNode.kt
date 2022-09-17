package org.orbit.precess.frontend.components.nodes

import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition

interface PropositionExpressionNode : IPrecessNode {
    fun getProposition(interpreter: Interpreter) : Proposition
}

fun PropositionExpressionNode.toPropositionNode(id: String) : PropositionNode
    = PropositionNode(firstToken, lastToken, id, this)