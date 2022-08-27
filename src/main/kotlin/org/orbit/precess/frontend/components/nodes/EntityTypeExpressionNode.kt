package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType

data class EntityTypeExpressionNode(override val firstToken: Token, override val lastToken: Token, val name: String) : TypeExpressionNode<IType.Type>() {
    override fun getChildren(): List<Node> = emptyList()
    override fun toString(): String = name

    override fun infer(env: Env): IType.Type = when (val t = env.getElementAs<IType.Type>(name)) {
        null -> IType.Type(name)
        else -> t
    }
}
