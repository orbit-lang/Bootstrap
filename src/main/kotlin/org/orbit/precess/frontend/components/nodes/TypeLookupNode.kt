package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class TypeLookupNode(override val firstToken: Token, override val lastToken: Token, val context: ContextLiteralNode, val type: TypeLiteralNode) : TypeExprNode() {
    override fun getChildren(): List<Node> = listOf(context, type)
    override fun toString(): String = "$context.$type"
}
