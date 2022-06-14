package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.parseTrailing
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
			MirrorRule, ExpandRule, LambdaLiteralRule, ReferenceCallRule, ConstructorRule, LiteralRule(), MethodCallRule, UnaryExpressionRule
		)
	}

	override fun parse(context: Parser) : ParseRule.Result {
 		val start = context.peek()

		val lhs = when (start.type) {
			TokenTypes.LParen -> {
				context.expect(TokenTypes.LParen)
				val expr = context.attemptAny(*valueRules)
					as? ExpressionNode
					?: TODO("HRIOIKLHASDF")
				context.expect(TokenTypes.RParen)
				expr
			}

			else -> context.attemptAny(*valueRules)
				as? ExpressionNode
				?: TODO("KL:JFASD8")
		}

		var next = context.peek()

		if (next.type == TokenTypes.OperatorSymbol) {
			val op = context.consume()
			val rhs = context.attempt(defaultValue)
				?: TODO("KLFHAS*")

			var bin = BinaryExpressionNode(start, rhs.lastToken, op.text, lhs, rhs)
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

		return +lhs

//		val start = context.peek()
//		var isGrouped = false
//
//		if (start.type == TokenTypes.LParen) {
//			// Grouped expression, e.g. `(2 + 2)`
//			isGrouped = true
//			context.consume()
//		}
//
//		val expr = context.attemptAny(*valueRules) as? ExpressionNode
//			?: return ParseRule.Result.Failure.Rewind(listOf(start))
//
//		val next = context.peek()
//
//		if (isGrouped && next.type == TokenTypes.RParen) {
//			context.consume()
//			return parseTrailing(context, expr)
//		}
//
//		if (next.type == TokenTypes.OperatorSymbol) {
//			try {
//				val partialExpressionRule = PartialExpressionRule(expr)
//
//				return partialExpressionRule.execute(context)
//			} finally {
//				if (isGrouped) context.expect(TokenTypes.RParen)
//			}
//		} else if (TokenTypes.Dot(next)) {
//			try {
//				val callRule = PartialCallRule(expr)
//
//				return callRule.execute(context)
//			} finally {
//				if (isGrouped) context.expect(TokenTypes.RParen)
//			}
//		}
//
//		if (isGrouped) context.expect(TokenTypes.RParen)
//
//		return ParseRule.Result.Success(expr)
	}
}
