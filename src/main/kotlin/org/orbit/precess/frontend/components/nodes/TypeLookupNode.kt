package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.phase.Interpreter
import org.orbit.precess.backend.utils.AnyEntity
import org.orbit.precess.backend.utils.AnyType

data class TypeLookupNode(override val firstToken: Token, override val lastToken: Token, val type: TypeLiteralNode) : TermExpressionNode<Expr.TypeLiteral>() {
    override fun getChildren(): List<Node> = listOf(type)
    override fun toString(): String = "∆.$type"

    override fun getExpression(): Expr.TypeLiteral
        = Expr.TypeLiteral(type.typeId)
}
