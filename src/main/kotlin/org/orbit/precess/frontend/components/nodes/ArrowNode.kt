package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.AnyType
import org.orbit.precess.backend.utils.TypeUtils

data class ArrowNode(override val firstToken: Token, override val lastToken: Token, val domain: TermExpressionNode<*>, val codomain: TermExpressionNode<*>) : TermExpressionNode<Expr.Arrow1>() {
    override fun getChildren(): List<Node> = listOf(domain, codomain)
    override fun toString(): String = "($domain) -> $codomain"

    override fun getExpression(env: Env): Expr.Arrow1
        = Expr.Arrow1(domain.getExpression(env), codomain.getExpression(env))
}
