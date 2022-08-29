package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr

data class TypeLookupNode(override val firstToken: Token, override val lastToken: Token, val type: TypeExpressionNode) : TermExpressionNode<Expr.AnyTypeLiteral>() {
    override fun getChildren(): List<Node> = listOf(type)
    override fun toString(): String = "$type"

    override fun getExpression(env: Env): Expr.AnyTypeLiteral
        = Expr.AnyTypeLiteral(type.infer(env))
}
