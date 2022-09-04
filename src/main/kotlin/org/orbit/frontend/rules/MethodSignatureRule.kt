package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.core.components.SourcePosition
import org.orbit.frontend.components.ParseError
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.phase.Parser
import java.util.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.util.Invocation

class MethodSignatureRule(private val anonymous: Boolean, private val autogeneratedName: String? = null) : ParseRule<MethodSignatureNode> {
	private companion object : KoinComponent {
		private val invocation: Invocation by inject()
	}

	sealed class Errors {
		data class UnexpectedAnonymous(override val sourcePosition: SourcePosition)
			: ParseError("Anonymous method signatures are not allowed at top level", sourcePosition)

		data class ExpectedAnonymous(override val sourcePosition: SourcePosition)
			: ParseError("Unexpected method name, this signature should be anonymous", sourcePosition)

		data class MissingReceiver(override val sourcePosition: SourcePosition)
			: ParseError("Expected receiver type (T) or (t T) following '(' in method signature", sourcePosition)
		
		data class MissingParameters(override val sourcePosition: SourcePosition)
			: ParseError("	Expected parameter list following '(' in method signature", sourcePosition)

		data class MissingReturnType(override val sourcePosition: SourcePosition)
			: ParseError("Expected return type following '(' in method signature", sourcePosition)
	}

	override fun parse(context: Parser) : ParseRule.Result {
		val autoName = autogeneratedName ?: UUID.randomUUID().toString()
		
		val start = context.expect(TokenTypes.LParen)

		val receiverNode = context.attemptAny(PairRule, TypeExpressionRule)
			?: throw context.invocation.make(Errors.MissingReceiver(context.peek().position))
		val isInstanceMethod = receiverNode.first != null

		context.expect(TokenTypes.RParen)

		val idStart = context.peek()
		val identifierNode = context.attempt(IdentifierRule)
		
		if (anonymous && identifierNode != null) {
			throw context.invocation.make(Errors.UnexpectedAnonymous(context.peek().position))
		} else if (!anonymous && identifierNode == null) {
			throw context.invocation.make(Errors.ExpectedAnonymous(context.peek().position))
		}

		var next = context.peek()

		val typeParameters: TypeParametersNode? = when(next.type) {
			TokenTypes.LAngle -> {
				context.attempt(TypeParametersRule)
					?: throw invocation.make<Parser>("~TODO~", next)
			}

			else -> null
		}

		next = context.peek()

		val parameterNodes = when (next.text) {
			"_" -> {
				context.consume()
				emptyList()
			}

			else -> {
				val delimitedRule = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, PairRule)
				context.attempt(delimitedRule)
					?.nodes
					?: throw context.invocation.make(Errors.MissingParameters(next.position))
			}
		}

		next = context.peek()

		var end = context.peek()
		val returnTypeNode = when (next.text) {
			"_" -> {
				context.consume()
				InferNode(next, next)
			}
			else -> {
				context.expect(TokenTypes.LParen)
				if (context.peek().type == TokenTypes.RParen) {
					end = context.expect(TokenTypes.RParen)
					null
				} else {
					val ret = context.attempt(TypeExpressionRule)
						?: throw context.invocation.make(Errors.MissingReturnType(next.position))

					end = context.expect(TokenTypes.RParen)

					ret
				}
			}
		}

		val id = identifierNode ?: IdentifierNode(idStart, idStart, autoName)

		return +when(receiverNode.first) {
			null -> MethodSignatureNode(start, end, id, receiverNode.second!!, parameterNodes, returnTypeNode, typeParameters, emptyList(), isInstanceMethod)
			else -> MethodSignatureNode(start, end, id, receiverNode.first!!.typeExpressionNode, listOf(receiverNode.first!!).plus(parameterNodes), returnTypeNode, typeParameters, emptyList(), isInstanceMethod)
		}
	}
}
