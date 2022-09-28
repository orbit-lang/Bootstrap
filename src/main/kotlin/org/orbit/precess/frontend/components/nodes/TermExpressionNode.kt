package org.orbit.precess.frontend.components.nodes

import org.orbit.backend.typesystem.components.Env
import org.orbit.precess.backend.components.Expr

interface TermExpressionNode<E: Expr<E>> : IPrecessNode {
    fun getExpression(env: Env) : E
}