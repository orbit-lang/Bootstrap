package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

data class BindingLiteralNode(override val firstToken: Token, override val lastToken: Token, val ref: RefLiteralNode, val type: TermExpressionNode<*>) : DeclNode<Decl.Assignment>() {
    override fun getChildren(): List<Node> = listOf(ref, type)
    override fun toString(): String = "$ref:$type"

    override fun getDecl(env: Env): DeclResult<Decl.Assignment> {
        val tType = when (val t = type.getExpression().infer(env)) {
            is IType.Never -> return DeclResult.Failure(t)
            else -> t
        }

        if (env.getRef(ref.refId) != null) return DeclResult.Failure(IType.Never("Type `${tType.id}` is already defined in current context: `$env`"))

        return DeclResult.Success(Decl.Assignment(ref.refId, tType))
    }
}
