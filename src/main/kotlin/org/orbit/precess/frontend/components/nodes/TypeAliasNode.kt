package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Decl
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

data class TypeAliasNode(override val firstToken: Token, override val lastToken: Token, val ref: String, val type: TypeExpressionNode<*>) : DeclNode<Decl.TypeAlias>() {
    override fun getChildren(): List<Node> = listOf(type)
    override fun toString(): String = "$ref:$type"

    override fun getDecl(env: Env): DeclResult<Decl.TypeAlias> = when (val res = type.infer(env)) {
        is IType.Never -> DeclResult.Failure(res)
        else -> DeclResult.Success(Decl.TypeAlias(ref, res))
    }
}

