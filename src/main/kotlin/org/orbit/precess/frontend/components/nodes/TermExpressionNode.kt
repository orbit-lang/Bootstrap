package org.orbit.precess.frontend.components.nodes

import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr

abstract class TermExpressionNode<E: Expr<E>> : Node() {
    abstract fun getExpression(env: Env) : E
}