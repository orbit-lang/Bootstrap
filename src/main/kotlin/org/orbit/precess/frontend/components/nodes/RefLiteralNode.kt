package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

data class RefLiteralNode(override val firstToken: Token, override val lastToken: Token, val refId: String) : Node() {
    override fun getChildren(): List<Node> = emptyList()
    override fun toString(): String = refId
}
