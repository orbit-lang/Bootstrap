package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class ArrowNode(override val firstToken: Token, override val lastToken: Token, val domain: TypeExprNode, val codomain: TypeExprNode) : TypeExprNode() {
    override fun getChildren(): List<Node> = listOf(domain, codomain)
    override fun toString(): String = "($domain) -> $codomain"
}
