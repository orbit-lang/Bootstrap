package org.orbit.precess.frontend.components.nodes

import org.orbit.core.nodes.INode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr

interface TermExpressionNode<E: Expr<E>> : INode {
    fun getExpression(env: Env) : E
}