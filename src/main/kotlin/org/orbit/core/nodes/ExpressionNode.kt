package org.orbit.core.nodes

import org.orbit.core.Token

/**
	This is really a "phantom" type, that allows us
	to be a bit stricter about typechecking the compiler
	itself. ExpressionNode, ExpressionRule & ValueNode
	only really "exist" nominally, that have no unique
	behaviour of their own (at least at parse-time).
*/
abstract class ExpressionNode(
	override val firstToken: Token,
	override val lastToken: Token
) : Node(firstToken, lastToken)