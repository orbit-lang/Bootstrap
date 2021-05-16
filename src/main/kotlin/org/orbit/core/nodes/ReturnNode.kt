package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ReturnStatementNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val valueNode: RValueNode
) : Node(firstToken, lastToken) {
	constructor(firstToken: Token, lastToken: Token, expressionNode: ExpressionNode)
		:this (firstToken, lastToken, RValueNode(expressionNode))

	override fun getChildren() : List<Node>
		= listOf(valueNode)
}