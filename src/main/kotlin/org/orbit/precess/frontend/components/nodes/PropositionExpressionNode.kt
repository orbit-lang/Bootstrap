package org.orbit.precess.frontend.components.nodes

import org.orbit.core.nodes.INode
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition

interface PropositionExpressionNode : INode {
    fun getProposition(interpreter: Interpreter) : Proposition
}