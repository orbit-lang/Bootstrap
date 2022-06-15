package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

abstract class TypeParameterRule : ParseRule<AbstractTypeParameterNode>

private object BoundedTypeParameterRule : TypeParameterRule() {
	override fun parse(context: Parser) : ParseRule.Result {
		val nameNode = TypeIdentifierRule.Naked
			.execute(context)
			.asSuccessOrNull<TypeIdentifierNode>()
			?.node
			?: return ParseRule.Result.Failure.Abort

		val next = context.peek()

		if (next.type == TokenTypes.Colon) {
			context.consume()

			/*
				NOTE - This is an interesting case!

				EXAMPLE:
					`type A<B: C<D>>`

				In this context, `C<D>` is an rval type, so we must parse it as a literal
			 */
			val boundNode = LiteralRule(TypeIdentifierRule.LValue)
				.execute(context)
				.asSuccessOrNull<RValueNode>()
				?.node
				?: return ParseRule.Result.Failure.Abort

			return +BoundedTypeParameterNode(nameNode.firstToken, boundNode.lastToken, nameNode, boundNode)
		}

		return +BoundedTypeParameterNode(nameNode.firstToken, nameNode.lastToken, nameNode)
	}
}

private object DependentTypeParameterRule : TypeParameterRule() {
	override fun parse(context: Parser) : ParseRule.Result {
		val nameNode = TypeIdentifierRule.Naked
			.execute(context)
			.asSuccessOrNull<TypeIdentifierNode>()
			?.node
			?: return ParseRule.Result.Failure.Abort

		val typeNode = LiteralRule(TypeIdentifierRule.LValue)
			.execute(context)
			.asSuccessOrNull<RValueNode>()
			?.node
			?: return ParseRule.Result.Failure.Abort

		return +DependentTypeParameterNode(nameNode.firstToken, typeNode.lastToken, nameNode, typeNode)
	}
}

private object ValueTypeParameterRule : TypeParameterRule() {
	override fun parse(context: Parser): ParseRule.Result {
		val valueNode = LiteralRule()
			.execute(context)
			.asSuccessOrNull<RValueNode>()
			?.node
			?: return ParseRule.Result.Failure.Abort

		return +ValueTypeParameterNode(valueNode.firstToken, valueNode.lastToken, valueNode)
	}
}

object TypeParametersRule : ParseRule<TypeParametersNode>, KoinComponent {
	private val invocation: Invocation by inject()

	override fun parse(context: Parser): ParseRule.Result {
		val delimitedRule = DelimitedRule(TokenTypes.LAngle, TokenTypes.RAngle, TypeIdentifierRule.Naked)
		val typeParameters = context.attempt(delimitedRule)
			?: throw invocation.make<Parser>("", context.peek())

		return +TypeParametersNode(typeParameters.firstToken, typeParameters.lastToken, typeParameters.nodes)
	}
}

class TypeParametersRule_Old(private val isRValueContext: Boolean) : ParseRule<TypeParametersNode> {
	override fun parse(context: Parser) : ParseRule.Result {
		var typeParameterNodes = mutableListOf<AbstractTypeParameterNode>()

		val start = context.expect(TokenTypes.LAngle)

		var next = context.peek()
		if (next.type == TokenTypes.RAngle) {
			// Empty generic expression is meaningless
			// TODO - Could `<>` mean same as `<*>` in Kotlin?
			TODO("@TypeParameterRule:64")
		}

		var end = next

		val lookaheadParser = Parser(context.invocation, DependentTypeParameterRule)

		while (next.type != TokenTypes.RAngle) {
			// Parsing precedence is critical here, must check for `<N Int>` style expressions before `N` or `N: Int`,
			// otherwise we end up with an ambiguity in the grammar
			if (isRValueContext) {
				// We're instantiating a generic type in an rval position, so only concrete Types/Values are allowed
				val typeParameterNode = ValueTypeParameterRule
					.execute(context)
					.asSuccessOrNull<AbstractTypeParameterNode>()
					?.node
					?: return ParseRule.Result.Failure.Abort

				typeParameterNodes.add(typeParameterNode)

				next = context.peek()

				if (next.type == TokenTypes.Comma) {
					next = context.consume()
				} else {
					context.expect(TokenTypes.RAngle)
					next = context.peek()
					break
				}
			} else {
				// Type parameters are in an lval position, meaning we're
				// declaring something, so concrete Types/Values are not allowed
				if (next.type != TokenTypes.TypeIdentifier) {
					TODO("@TypeParameterRule:93")
				}

				var result: Parser.Result? = null
				var typeParameterNode: AbstractTypeParameterNode

				try {
					result = lookaheadParser.execute(Parser.InputType(context.tokens))

					typeParameterNode = if (result.ast is DependentTypeParameterNode) {
						// We need to manually remove the tokens parsed by the lookahead parser
						context.consume(result.ast.firstToken, result.ast.lastToken)
						result.ast as DependentTypeParameterNode
					} else {
						context.attempt(BoundedTypeParameterRule)
							?: TODO("@TypeParameterRule:108")
					}
				} catch (_: Exception) {
					if (result != null) {
						context.consume(result.ast.firstToken, result.ast.lastToken)
					}

					typeParameterNode = context.attempt(BoundedTypeParameterRule, true)
						?: TODO("@TypeParameterRule:116")
				}

				typeParameterNodes.add(typeParameterNode)

				end = typeParameterNode.lastToken

				next = context.peek()

				if (next.type == TokenTypes.Comma) {
					context.consume()
					next = context.peek()
				} else {
					end = context.expect(TokenTypes.RAngle)
					break
				}
			}
		}
		
		return +TypeParametersNode(start, end, emptyList())
	}
}