package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.Expr

data class EntityLookupNode(override val firstToken: Token, override val lastToken: Token, val context: ContextLiteralNode, val typeId: String) : LookupNode<Expr.Type> {
    override fun getChildren(): List<INode> = listOf(context)
    override fun toString(): String = typeId

    override fun getExpression(env: Env): Expr.Type
        = Expr.Type(typeId)
}