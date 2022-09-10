package org.orbit.core.nodes

import org.orbit.core.components.Token

/**
	This is really a "phantom" type, that allows us
	to be a bit stricter about typechecking the compiler
	itself. ExpressionNode, ExpressionRule & ValueNode
	only really "exist" nominally, that have no unique
	behaviour of their own (at least at parse-time).
*/
interface IExpressionNode : INode {
	fun toBlockNode() : BlockNode
		= BlockNode(firstToken, lastToken, listOf(this))
}

data class RValueNode(
	override val firstToken: Token,
	override val lastToken: Token,
	val expressionNode: IExpressionNode,
	val typeParametersNode: TypeParametersNode = TypeParametersNode(lastToken, lastToken)
) : IExpressionNode {
	constructor(expressionNode: IExpressionNode)
		: this(expressionNode.firstToken, expressionNode.lastToken, expressionNode)

	override fun getChildren() : List<INode> = listOf(expressionNode)
}