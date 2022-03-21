package org.orbit.core.nodes

import org.orbit.core.components.Token
import org.orbit.util.containsInstances

data class BlockNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val body: List<Node>
) : ExpressionNode(firstToken, lastToken), ValueRepresentableNode {
	val isEmpty: Boolean get() = body.isEmpty()

	val containsDefer: Boolean get() = body.containsInstances<DeferNode>()
	// TODO - Nested blocks may themselves return, which may not count towards this result
	//  Revisit once control-flow, lambdas etc are a thing
	val containsReturn: Boolean get() = body.containsInstances<ReturnStatementNode>()

	override fun getChildren() : List<Node> {
		return body
	}
}