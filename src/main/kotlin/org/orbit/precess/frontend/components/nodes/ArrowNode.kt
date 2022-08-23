package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.utils.AnyArrow
import org.orbit.precess.backend.utils.AnyType

data class ArrowNode(override val firstToken: Token, override val lastToken: Token, val domain: TermExpressionNode<*>, val codomain: TermExpressionNode<*>) : TermExpressionNode<Expr.ArrowLiteral>() {
    override fun getChildren(): List<Node> = listOf(domain, codomain)
    override fun toString(): String = "($domain) -> $codomain"

    override fun getExpression(): Expr.ArrowLiteral {
        val dType = domain.getExpression()
        val cType = codomain.getExpression()

        return Expr.ArrowLiteral(dType, cType)
    }
}
