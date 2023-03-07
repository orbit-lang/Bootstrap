package org.orbit.frontend.rules

import org.orbit.core.components.SourcePosition
import org.orbit.core.components.Token
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.BlockNode
import org.orbit.core.nodes.INode
import org.orbit.frontend.components.ParseError
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

class BlockRule(private vararg val bodyRules: ParseRule<*>) : ParseRule<BlockNode> {
	sealed class Errors {
		class UnexpectedBlockStatement(private val token: Token, override val sourcePosition: SourcePosition = token.position)
			: ParseError("Unexpected token inside block: ${token.type}", sourcePosition)
	}

	companion object {
		val default = BlockRule(CauseRule, MirrorRule, TypeOfRule, RefOfRule, ContextOfRule, DeferRule, PrintRule, ReturnRule, AssignmentRule, LambdaLiteralRule, MethodCallRule)
		val lambda = BlockRule(CauseRule, MirrorRule, TypeOfRule, RefOfRule, ContextOfRule, DeferRule, PrintRule, ReturnRule, AssignmentRule, LambdaLiteralRule, ExpressionRule.defaultValue)
		val methodBody = BlockRule(CauseRule, CheckRule, MirrorRule, TypeOfRule, RefOfRule, ContextOfRule, DeferRule, PrintRule, ReturnRule, AssignmentRule, MethodCallRule)
	}
	
	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.expect(TokenTypes.LBrace)
		var next = context.peek()
		val body = mutableListOf<INode>()
		
		while (next.type != TokenTypes.RBrace) {
			body.add(context.attemptAny(*bodyRules, throwOnNull = true)
				?: throw context.invocation.make(Errors.UnexpectedBlockStatement(next)))

			next = context.peek()
		}

		val end = context.expect(TokenTypes.RBrace)
		
		return +BlockNode(start, end, body)
	}
}