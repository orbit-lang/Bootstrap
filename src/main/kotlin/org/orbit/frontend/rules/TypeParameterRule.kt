package org.orbit.frontend.rules

import org.orbit.core.nodes.*
import org.orbit.frontend.rules.*
import org.orbit.frontend.rules.PairRule
import org.orbit.core.SourcePosition
import org.orbit.core.Warning
import org.orbit.frontend.*

private object BoundedTypeParameterRule : ParseRule<TypeParameterNode> {
	override fun parse(context: Parser) : TypeParameterNode {
		val nameNode = context.attempt(TypeIdentifierRule)
			?: throw Exception("TODO")

		val next = context.peek()

		if (next.type == TokenTypes.Colon) {
			context.consume()
			
			val boundNode = context.attempt(TypeIdentifierRule)
				?: throw Exception("TODO")

			return BoundedTypeParameterNode(nameNode.firstToken, boundNode.lastToken, nameNode, boundNode)
		}

		return BoundedTypeParameterNode(nameNode.firstToken, nameNode.lastToken, nameNode)
	}
}

private object DependentTypeParameterRule : ParseRule<TypeParameterNode> {
	override fun parse(context: Parser) : TypeParameterNode {
		val nameNode = context.attempt(TypeIdentifierRule)
			?: throw Exception("TODO")

		val typeNode = context.attempt(TypeIdentifierRule)
			?: throw Exception("TODO")

		return DependentTypeParameterNode(nameNode.firstToken, typeNode.lastToken, nameNode, typeNode)
	}
}

object TypeParametersRule : ParseRule<TypeParametersNode> {
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

		val lookaheadParser = Parser(DependentTypeParameterRule)

		while (next.type != TokenTypes.RAngle) {
			// Parsing precedence is critical here, must check for `<N Int>` style expressions before `N` or `N: Int`,
			// otherwise we end up with an ambiguity in the grammar
			if (next.type != TokenTypes.TypeIdentifier) {
				throw Exception("TODO")
			}

			var result: ParseResult? = null
			var typeParameterNode: TypeParameterNode

			try {
				result = lookaheadParser.execute(context.tokens)

				if (result.ast is DependentTypeParameterNode) {
					// We need to manually remove the tokens parsed by the lookahead parser
					context.consume(result.ast.firstToken, result.ast.lastToken)
					typeParameterNode = result.ast as DependentTypeParameterNode
				} else {
					typeParameterNode = context.attempt(BoundedTypeParameterRule)
						?: throw Exception("TODO")
				}
			} catch (_: Exception) {
				if (result != null) {
					context.consume(result.ast.firstToken, result.ast.lastToken)
				}

				typeParameterNode = context.attempt(BoundedTypeParameterRule)
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
		
		return TypeParametersNode(start, end, typeParameterNodes)
	}
}