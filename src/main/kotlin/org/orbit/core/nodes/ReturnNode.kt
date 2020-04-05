package org.orbit.core.nodes

import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.Parser
import org.orbit.frontend.ParseRule
import org.orbit.core.Token

data class ReturnStatementNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val valueNode: ExpressionNode
) : Node(firstToken, lastToken) {
	override fun getChildren() : List<Node>
		= listOf(valueNode)
}