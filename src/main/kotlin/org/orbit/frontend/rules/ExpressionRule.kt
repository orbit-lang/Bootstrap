package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

interface ValueRule<E: ExpressionNode> : ParseRule<E>

class ExpressionRule(vararg val valueRules: ValueRule<*>) : ParseRule<ExpressionNode> {
	companion object {
		val defaultValue = ExpressionRule(
			MirrorRule,
			ExpandRule,
			MethodCallRule,
			LambdaLiteralRule,
			ReferenceCallRule,
			UnaryExpressionRule,
			ConstructorRule,
			LiteralRule()
		)

		val singleExpressionBodyRule = ExpressionRule(
			MirrorRule, ExpandRule, LambdaLiteralRule, ReferenceCallRule, ConstructorRule, LiteralRule(), MethodCallRule//, UnaryExpressionRule
		)
	}

	override fun parse(context: Parser) : ParseRule.Result {
 		val start = context.peek()

		val lhs = when (start.type) {
			TokenTypes.LParen -> {
				context.expect(TokenTypes.LParen)
				val expr = context.attempt(ExpressionRule(*valueRules))
					?: TODO("HRIOIKLHASDF")
				context.expect(TokenTypes.RParen)
				expr
			}

			TokenTypes.OperatorSymbol -> {
				// Prefix operator
				val op = context.consume()
				val expr = context.attemptAny(*valueRules)
					as? ExpressionNode
					?: TODO("KJ LDJ")

				UnaryExpressionNode(op, expr.lastToken, op.text, expr, OperatorFixity.Prefix)
			}

			else -> context.attemptAny(*valueRules)
				as? ExpressionNode
				?: context.protected<ExpressionNode> { TODO("KL:JFASD8: ${start.type}") }
		}

		var next = context.peek()

		if (next.type == TokenTypes.Dot) {
			context.consume()
			val message = context.attempt(IdentifierRule)
				?: TODO("JKL:JFIO *(Y")

			next = context.peek()

			if (next.type == TokenTypes.LParen) {
				val delim = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, ExpressionRule.defaultValue)
				val delimResult = context.attempt(delim)
					?: TODO("lj ;klsj df")

				return +MethodCallNode(start, delimResult.lastToken, lhs!!, message, delimResult.nodes, false)
			}

			return +MethodCallNode(start, message.lastToken, lhs!!, message, emptyList(), true)
		} else if (next.type == TokenTypes.OperatorSymbol) {
			val op = context.consume()
			context.mark()

			context.setThrowProtection(true)
			val rhs = try {
				context.attempt(defaultValue)
			} catch (_: Exception) {
				null
			}
			context.setThrowProtection(false)
			val recorded = context.end()

			if (rhs == null) {
				// Postfix operator
				context.rewind(recorded)

				return +UnaryExpressionNode(start, lhs!!.lastToken, op.text.replace("`", ""), lhs!!, OperatorFixity.Postfix)
			}

			var bin = BinaryExpressionNode(start, rhs.lastToken, op.text.replace("`", ""), lhs!!, rhs)
			next = context.peek()
			while (next.type == TokenTypes.OperatorSymbol) {
				val nOp = context.consume()
				val expr = context.attempt(defaultValue)
					?: TODO("HKFJSDLKF*")
				bin = BinaryExpressionNode(start, expr.lastToken, nOp.text, bin, expr)

				next = context.peek()
			}

			return +bin
		}

		return +lhs!!
	}
}
