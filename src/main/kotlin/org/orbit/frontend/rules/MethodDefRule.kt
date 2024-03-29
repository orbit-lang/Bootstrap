package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.util.Invocation
import org.orbit.util.PrintableKey
import org.orbit.util.Printer

object MethodDefRule : ParseRule<MethodDefNode>, KoinComponent {
	private val invocation: Invocation by inject()
	private val printer: Printer by inject()

	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.peek()

		val signature = context.attempt(MethodSignatureRule(false), true)
			?: TODO("@MethodDefRule:18")

		var next = context.peek()

		var contextNode: IContextExpressionNode? = null
		if (next.type == TokenTypes.Within) {
			contextNode = context.attempt(ContextExpressionRule)
			next = context.peek()
		}

		if (next.type == TokenTypes.Assignment) {
			// Eat the '='
			context.consume()
			// Single expression method body

			val expression = context.attempt(ExpressionRule.singleExpressionBodyRule)
				?: TODO("Method Body Single Expression")

			val returnStatement = ReturnStatementNode(expression.firstToken, expression.lastToken, expression)
			val body = BlockNode(returnStatement.firstToken, returnStatement.lastToken, listOf(returnStatement))

			return +MethodDefNode(start, body.lastToken, signature, body, contextNode)
		}

		val body = context.attempt(BlockRule.methodBody, true)
			?: TODO("@MethodDefRule:24")

		return +MethodDefNode(start, body.lastToken, signature, body, contextNode)
	}
}