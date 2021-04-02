package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.rules.PairRule
import org.orbit.core.SourcePosition
import org.orbit.core.Warning
import org.orbit.frontend.*

abstract class TypeParameterRule() : ParseRule<TypeParameterNode>

private object BoundedTypeParameterRule : TypeParameterRule() {
	override fun parse(context: Parser) : TypeParameterNode {
		val nameNode = TypeIdentifierRule.Naked.execute(context)
		val next = context.peek()

		if (next.type == TokenTypes.Colon) {
			context.consume()

			/*
				NOTE - This is an interesting case!

				EXAMPLE:
					`type A<B: C<D>>`

				In this context, `C<D>` is an rval type, so we must parse it as a literal
			 */
			val boundNode = LiteralRule(TypeIdentifierRule.LValue).execute(context)

			return BoundedTypeParameterNode(nameNode.firstToken, boundNode.lastToken, nameNode, boundNode)
		}

		return BoundedTypeParameterNode(nameNode.firstToken, nameNode.lastToken, nameNode)
	}
}

private object DependentTypeParameterRule : TypeParameterRule() {
	override fun parse(context: Parser) : TypeParameterNode {
		val nameNode = TypeIdentifierRule.Naked.execute(context)
		val typeNode = LiteralRule(TypeIdentifierRule.LValue).execute(context)

		return DependentTypeParameterNode(nameNode.firstToken, typeNode.lastToken, nameNode, typeNode)
	}
}

private object ValueTypeParameterRule : TypeParameterRule() {
	override fun parse(context: Parser): TypeParameterNode {
		val valueNode = LiteralRule().execute(context)

		return ValueTypeParameterNode(valueNode.firstToken, valueNode.lastToken, valueNode)
	}
}

class TypeParametersRule(private val isRValueContext: Boolean) : ParseRule<TypeParametersNode> {
	override fun parse(context: Parser) : TypeParametersNode {
		var typeParameterNodes = mutableListOf<TypeParameterNode>()

		val start = context.expect(TokenTypes.LAngle)

		var next = context.peek()
		if (next.type == TokenTypes.RAngle) {
			// Empty generic expression is meaningless
			// TODO - Could `<>` mean same as `<*>` in Kotlin?
			throw Exception("TODO")
		}

		var end = next

		val lookaheadParser = Parser(context.invocation, DependentTypeParameterRule)

		while (next.type != TokenTypes.RAngle) {
			// Parsing precedence is critical here, must check for `<N Int>` style expressions before `N` or `N: Int`,
			// otherwise we end up with an ambiguity in the grammar
			if (isRValueContext) {
				// We're instantiating a generic type in an rval position, so only concrete Types/Values are allowed
				val typeParameterNode = ValueTypeParameterRule.execute(context)

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
					throw Exception("TODO")
				}

				var result: Parser.Result? = null
				var typeParameterNode: TypeParameterNode

				try {
					result = lookaheadParser.execute(Parser.InputType(context.tokens))

					typeParameterNode = if (result.ast is DependentTypeParameterNode) {
						// We need to manually remove the tokens parsed by the lookahead parser
						context.consume(result.ast.firstToken, result.ast.lastToken)
						result.ast as DependentTypeParameterNode
					} else {
						context.attempt(BoundedTypeParameterRule)
							?: throw Exception("TODO")
					}
				} catch (_: Exception) {
					if (result != null) {
						context.consume(result.ast.firstToken, result.ast.lastToken)
					}

					typeParameterNode = context.attempt(BoundedTypeParameterRule, true)
						?: throw Exception("TODO")
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
		
		return TypeParametersNode(start, end, typeParameterNodes)
	}
}