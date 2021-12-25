package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.extensions.unaryPlus

object MethodDefRule : ParseRule<MethodDefNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.peek()

		val signature = context.attempt(MethodSignatureRule(false), true)
			?: TODO("@MethodDefRule:18")

		val next = context.peek()

		if (next.type == TokenTypes.Assignment) {
			// Eat the '='
			context.consume()
			// Single expression method body
			val expression = context.attempt(ExpressionRule.singleExpressionBodyRule)
				?: TODO("Method Body Single Expression")

			val returnStatement = ReturnStatementNode(expression.firstToken, expression.lastToken, expression)
			val body = BlockNode(returnStatement.firstToken, returnStatement.lastToken, listOf(returnStatement))

			return +MethodDefNode(start, body.lastToken, signature, body)
		}

		val body = context.attempt(BlockRule.default, true)
			?: TODO("@MethodDefRule:24")

		return +MethodDefNode(start, body.lastToken, signature, body)
	}
}