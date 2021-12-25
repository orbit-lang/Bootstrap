package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ExtensionNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val targetTypeNode: TypeExpressionNode,
    val methodDefNodes: List<MethodDefNode>
) : Node(firstToken, lastToken) {
    override fun getChildren(): List<Node> = listOf(targetTypeNode) + methodDefNodes
}