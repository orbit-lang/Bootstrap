package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.core.components.SourcePosition
import org.orbit.frontend.components.ParseError
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.phase.Parser
import java.util.*
import org.orbit.frontend.extensions.unaryPlus

class MethodSignatureRule(private val anonymous: Boolean, private val autogeneratedName: String? = null) :
	ParseRule<MethodSignatureNode> {
	sealed class Errors {
		data class UnexpectedAnonymous(override val sourcePosition: SourcePosition)
			: ParseError("Anonymous method signatures are not allowed at top level", sourcePosition)

		data class ExpectedAnonymous(override val sourcePosition: SourcePosition)
			: ParseError("Unexpected method name, this signature should be anonymous", sourcePosition)

		data class MissingReceiver(override val sourcePosition: SourcePosition)
			: ParseError("Expected receiver type (T) or (t T) following '(' in method signature", sourcePosition)
		
		data class MissingParameters(override val sourcePosition: SourcePosition)
			: ParseError("Expected parameter list following '(' in method signature", sourcePosition)

		data class MissingReturnType(override val sourcePosition: SourcePosition)
			: ParseError("Expected return type following '(' in method signature", sourcePosition)
	}

	override fun parse(context: Parser) : ParseRule.Result {
		val autoName = autogeneratedName ?: UUID.randomUUID().toString()
		
		val start = context.expect(TokenTypes.LParen)

		val receiverNode = context.attemptAny(PairRule, TypeExpressionRule)
			?: throw context.invocation.make(Errors.MissingReceiver(context.peek().position))

		context.expect(TokenTypes.RParen)

		val idStart = context.peek()
		val identifierNode = context.attempt(IdentifierRule)
		
		if (anonymous && identifierNode != null) {
			throw context.invocation.make(Errors.UnexpectedAnonymous(context.peek().position))
		} else if (!anonymous && identifierNode == null) {
			throw context.invocation.make(Errors.ExpectedAnonymous(context.peek().position))
		}

		context.expect(TokenTypes.LParen)
		
		var next = context.peek()

		val parameterNodes = mutableListOf<PairNode>()

		if (next.type == TokenTypes.RParen) {
			context.consume()
		} else {
			while (true) {
				val pair = context.attempt(PairRule)
					?: throw context.invocation.make(MethodSignatureRule.Errors.MissingParameters(next.position))

				parameterNodes += pair

				next = context.peek()

				if (next.type == TokenTypes.Comma) {
					context.consume()
				} else {
					context.expect(TokenTypes.RParen)
					break
				}
			}
		}

		next = context.expect(TokenTypes.LParen)
		
		val returnTypeNode =
			if (context.peek().type == TokenTypes.RParen) {
				null
			} else {
				context.attempt(TypeExpressionRule)
					?: throw context.invocation.make(Errors.MissingReturnType(next.position))
			}

		val end = context.expect(TokenTypes.RParen)
		val id = identifierNode ?: IdentifierNode(idStart, idStart, autoName)

		return +when {
			receiverNode.first != null -> {
				MethodSignatureNode(start, end,
					id, receiverNode.first!!, parameterNodes, returnTypeNode)
			}
			receiverNode.second != null -> {
				MethodSignatureNode(start, end,
					id, receiverNode.second!!, parameterNodes, returnTypeNode)
			}
			
			else -> throw Exception("???")
		}
	}
}
