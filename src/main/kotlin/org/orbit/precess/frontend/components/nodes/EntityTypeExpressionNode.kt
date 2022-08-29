package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node
import org.orbit.precess.backend.components.Env
import org.orbit.precess.backend.components.IType
import org.orbit.precess.backend.utils.AnyType

data class EntityTypeExpressionNode(override val firstToken: Token, override val lastToken: Token, val name: String) : TypeExpressionNode() {
    override fun getChildren(): List<Node> = emptyList()
    override fun toString(): String = name

    override fun infer(env: Env): AnyType = when (val t = env.getElement(name)) {
        null -> IType.Type(name).exists(env)
        else -> t
    }
}
