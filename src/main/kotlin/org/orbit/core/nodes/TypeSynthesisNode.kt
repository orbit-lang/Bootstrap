package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.types.next.components.Kind

class TypeSynthesisNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val kind: Kind,
    val targetNode: TypeExpressionNode
) : TypeExpressionNode() {
    override val value: String = targetNode.value

    override fun getChildren(): List<Node> = listOf(targetNode)
}