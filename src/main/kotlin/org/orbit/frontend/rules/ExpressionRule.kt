package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.util.Invocation

interface ValueRule<E: IExpressionNode> : ParseRule<E>

object GroupedExpressionRule : ValueRule<IExpressionNode> {
	override fun parse(context: Parser): ParseRule.Result {
		context.mark()
		context.expect(TokenTypes.LParen)
		val expr = context.attempt(ExpressionRule.defaultValue)
			?: return ParseRule.Result.Failure.Rewind(context.end())
		context.expect(TokenTypes.RParen)

		return +expr
	}
}

class ExpressionRule(private vararg val valueRules: ValueRule<*>) : ParseRule<IExpressionNode>, KoinComponent {
	private val invocation: Invocation by inject()

	companion object {
		val defaultValue = ExpressionRule(
			PanicRule,
			SelectRule,
			MirrorRule,
			ExpandRule,
			ConstructorInvocationRule,
			MethodCallRule,
			LambdaLiteralRule,
			ReferenceCallRule,
			UnaryExpressionRule,
			LiteralRule(),
			AnonymousParameterRule
		)

		val singleExpressionBodyRule = ExpressionRule(
			AssignmentRule, PanicRule, SelectRule, MirrorRule, ExpandRule, LambdaLiteralRule, ReferenceCallRule, ConstructorInvocationRule, MethodCallRule, LiteralRule()
		)

		val selectConditionRule = ExpressionRule(
			ExpandRule, ConstructorInvocationRule, MethodCallRule, LiteralRule()
		)
	}

	override fun parse(context: Parser) : ParseRule.Result {
 		val start = context.peek()

		var lhs = when (start.type) {
			TokenTypes.LParen -> {
				// TODO - Not a big fan of shoehorning in special cases like this
				when (val tuple = context.attempt(TupleLiteralRule)) {
					null -> {
						val tok = context.expect(TokenTypes.LParen)
						val expr = context.attempt(ExpressionRule(*valueRules))
							?: throw invocation.make<Parser>("Expected expression after open parenthesis", tok)
						context.expect(TokenTypes.RParen)
						expr
					}

					else -> tuple
				}
			}

			TokenTypes.OperatorSymbol -> {
				// Prefix operator
				val op = context.consume()
				val next = context.peek()
				if (next.type == TokenTypes.LParen) {
					val delim = DelimitedRule(innerRule = ExpressionRule(*valueRules))
					val expr = context.attempt(delim)
						?: throw invocation.make<Parser>("Expected expression after Prefix Operator", op)

					UnaryExpressionNode(op, expr.lastToken, op.text.replace("`", ""), expr.nodes[0], OperatorFixity.Prefix)
				} else {
					val expr = context.attemptAny(*valueRules)
						as? IExpressionNode
						?: throw invocation.make<Parser>("Expected expression after Prefix Operator", op)

					UnaryExpressionNode(op, expr.lastToken, op.text.replace("`", ""), expr, OperatorFixity.Prefix)
				}
			}

			else -> context.attemptAny(*valueRules)
				as? IExpressionNode
				?: context.protected<IExpressionNode> {
					throw invocation.make<Parser>("Expected expression", context.peek())
				}
		}

		if (!context.hasMore) return +lhs!!

		var next = context.peek()

		if (next.type == TokenTypes.Dot) {
			context.consume()
			val message = context.attempt(IdentifierRule)
				?: throw invocation.make<Parser>("Expected Identifier after on right-hand side of call expression", context.peek())

			next = context.peek()

			if (next.type == TokenTypes.LParen) {
				val delim = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, defaultValue)
				val delimResult = context.attempt(delim)
					?: throw invocation.make<Parser>("Expected argument list after open parenthesis", context.peek())

				return +MethodCallNode(start, delimResult.lastToken, lhs!!, message, delimResult.nodes, false)
			}

			lhs = MethodCallNode(start, message.lastToken, lhs!!, message, emptyList(), true)

			if (!context.hasMore) {
				return +lhs
			}

			next = context.peek()
		}

		if (next.type == TokenTypes.OperatorSymbol) {
			val op = context.consume()
			context.mark()

			context.setThrowProtection(true)
			val rhs = try {
				context.attemptAny(listOf(GroupedExpressionRule, defaultValue))
					as IExpressionNode
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
					?: throw invocation.make<Parser>("Expected expression on right-hand side of binary expression", context.peek())

				bin = BinaryExpressionNode(start, expr.lastToken, nOp.text, bin, expr)
				next = context.peek()
			}

			return +bin
		}

		return +lhs!!
	}
}
