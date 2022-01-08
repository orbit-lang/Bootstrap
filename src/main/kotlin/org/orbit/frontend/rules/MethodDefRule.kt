package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.frontend.components.TokenTypes
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

		val next = context.peek()

		if (next.type == TokenTypes.Assignment) {
			// Eat the '='
			context.consume()
			// Single expression method body

			if (context.peek().type == TokenTypes.Invoke) {
				// TODO - This is a dirty hack to get around a grammar ambiguity
				//  when parsing a single expression method body that consists of e.g.
				//  `invoke { x }`, followed by another method definition
				throw invocation.make<Parser>("Where a single-expression method body consists of `invoke { ... }`, the expression must be grouped: `(invoke { ... })` to avoid ambiguity.\n${printer.apply("NOTE: this restriction might be relaxed in future versions.", PrintableKey.Italics)}", context.peek())
			}

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