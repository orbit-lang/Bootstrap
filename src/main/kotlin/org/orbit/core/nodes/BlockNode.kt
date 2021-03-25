package org.orbit.core.nodes

import org.orbit.core.Token
import org.orbit.graph.Binding
import org.orbit.graph.Environment
import org.orbit.types.Context
import org.orbit.types.TypeResolver

data class BlockNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val body: List<Node>) : Node(firstToken, lastToken) {
	val isEmpty: Boolean get() = body.isEmpty()

	override fun getChildren() : List<Node> {
		return body
	}
}