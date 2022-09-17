package org.orbit.core.nodes

import org.orbit.core.components.Token

data class ReturnStatementNode(
    override val firstToken: Token,
    override val lastToken: Token,
    val valueNode: RValueNode
) : IMethodBodyStatementNode {
	constructor(firstToken: Token, lastToken: Token, expressionNode: IExpressionNode)
		:this (firstToken, lastToken, RValueNode(expressionNode))

	override fun getChildren() : List<INode>
		= listOf(valueNode)
}