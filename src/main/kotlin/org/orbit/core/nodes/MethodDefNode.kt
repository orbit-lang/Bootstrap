package org.orbit.core.nodes

import org.orbit.core.Token

data class MethodDefNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val signature: MethodSignatureNode,
	val body: BlockNode
) : Node(firstToken, lastToken) {
	override fun getChildren() : List<Node>
		= listOf(signature, body)
}