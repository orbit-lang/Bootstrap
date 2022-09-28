package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.backend.typesystem.components.Decl
import org.orbit.backend.typesystem.components.Env
import org.orbit.backend.typesystem.components.IType
import org.orbit.backend.typesystem.components.TypeAttribute

data class BindingLiteralNode(override val firstToken: Token, override val lastToken: Token, val ref: RefLiteralNode, val term: TermExpressionNode<*>) : DeclNode<Decl.Assignment> {
    override fun getChildren(): List<INode> = listOf(ref, term)
    override fun toString(): String = "$ref:$term"

    override fun getDecl(env: Env): DeclResult<Decl.Assignment> {
        val tType = when (val t = term.getExpression(env).infer(env).exists(env)) {
            is IType.Never -> return DeclResult.Failure(t)
            is IType.Type -> when (t.attributes.contains(TypeAttribute.Uninhabited)) {
                true -> return DeclResult.Failure(IType.Never("Cannot bind to uninhabited Type `${t.id}`"))
                else -> t
            }
            else -> t
        }

        return DeclResult.Success(Decl.Assignment(ref.refId, tType))
    }
}
