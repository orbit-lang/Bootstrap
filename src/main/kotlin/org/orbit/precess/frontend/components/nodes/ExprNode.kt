package org.orbit.precess.frontend.components.nodes

import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.utils.AnyExpr
import org.orbit.precess.backend.utils.AnyType

abstract class ExprNode : Node() {
    abstract fun infer(interpreter: Interpreter, env: Env) : AnyType
}