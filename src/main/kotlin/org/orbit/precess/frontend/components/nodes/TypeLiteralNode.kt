package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.Node

abstract class DeclNode : Node()

data class TypeLiteralNode(override val firstToken: Token, override val lastToken: Token, val typeId: String) : DeclNode() {
    override fun getChildren(): List<Node> = emptyList()
    override fun toString(): String = typeId
}
