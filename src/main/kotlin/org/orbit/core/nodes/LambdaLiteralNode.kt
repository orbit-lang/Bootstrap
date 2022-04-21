package org.orbit.core.nodes

import org.orbit.core.components.Token

class LambdaLiteralNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val bindings: List<PairNode>,
    val body: BlockNode
) : ExpressionNode(), ValueRepresentableNode {
    override fun getChildren(): List<Node>
        = bindings + body
}