package org.orbit.precess.frontend.components.nodes

import org.orbit.core.components.Token
import org.orbit.core.nodes.INode

data class RefLiteralNode(override val firstToken: Token, override val lastToken: Token, val refId: String) : IPrecessNode {
    override fun getChildren(): List<INode> = emptyList()
    override fun toString(): String = refId
}
