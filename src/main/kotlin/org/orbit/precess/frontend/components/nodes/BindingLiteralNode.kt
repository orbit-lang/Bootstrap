package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr

data class BindingLiteralNode(override val firstToken: Token, override val lastToken: Token, val ref: RefLiteralNode, val type: TermExpressionNode<*>) : DeclNode<Decl.Assignment>() {
    override fun getChildren(): List<Node> = listOf(ref, type)
    override fun toString(): String = "$ref:$type"

    override fun getDecl(env: Env): Decl.Assignment {
        val t = type.getExpression(env).infer(env)

        if (env.getRef(ref.refId) != null) throw Exception("`${t.id}` is already defined in current context: `$env`")

        return Decl.Assignment(ref.refId, Expr.TypeLiteral(t.id))
    }
}
