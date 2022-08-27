package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.components.TypeAttribute
import org.orbit.precess.backend.utils.AnyType

data class BindingLiteralNode(override val firstToken: Token, override val lastToken: Token, val ref: RefLiteralNode, val type: TypeExpressionNode<*>) : DeclNode<Decl.Assignment>() {
    override fun getChildren(): List<Node> = listOf(ref, type)
    override fun toString(): String = "$ref:$type"

    override fun getDecl(env: Env): DeclResult<Decl.Assignment> {
        val tType = when (val t = type.infer(env).exists(env)) {
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
