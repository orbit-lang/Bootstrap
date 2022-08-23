package org.orbit.precess.frontend.components.nodes

import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.phase.Proposition
import org.orbit.precess.backend.utils.AnyExpr
import org.orbit.precess.backend.utils.AnyType

abstract class PropositionExpressionNode : Node() {
    abstract fun getProposition(interpreter: Interpreter) : Proposition
}