package org.orbit.frontend.rules

import org.orbit.core.components.SourcePosition
import org.orbit.frontend.components.ParseError
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.extensions.unaryPlus

object StructTypeRule : ValueRule<StructTypeNode> {
	override fun parse(context: Parser): ParseRule.Result {
		val delim = DelimitedRule(TokenTypes.LBrace, TokenTypes.RBrace, PairRule)
		val delimResult = context.attempt(delim)
			?: return ParseRule.Result.Failure.Abort

		return +StructTypeNode(delimResult.firstToken, delimResult.lastToken, delimResult.nodes)
	}
}

object TupleTypeRule : ValueRule<TupleTypeNode> {
	override fun parse(context: Parser): ParseRule.Result {
		val collector = context.startCollecting()
		val start = context.expect(TokenTypes.LParen)
		val left = context.attempt(TypeExpressionRule)
			?: return ParseRule.Result.Failure.Rewind(collector)

		context.expect(TokenTypes.Comma)

		val right = context.attempt(TypeExpressionRule)
			?: return ParseRule.Result.Failure.Rewind(collector)

		val end = context.expect(TokenTypes.RParen)

		return +TupleTypeNode(start, end, left, right)
	}
}

object CollectionTypeRule : ValueRule<CollectionTypeNode> {
	override fun parse(context: Parser): ParseRule.Result {
		context.mark()
		val collector = context.startCollecting()
		val start = context.expect(TokenTypes.LBracket)
		val elementType = context.attempt(TypeExpressionRule)
			?: return ParseRule.Result.Failure.Rewind(collector.getCollectedTokens())

		val end = context.expect(TokenTypes.RBracket)

		return +CollectionTypeNode(start, end, elementType)
	}
}

object TypeExpressionRule : ValueRule<TypeExpressionNode> {
	override fun parse(context: Parser): ParseRule.Result {
		context.mark()
		val collector = context.startCollecting()
		val node = context.attemptAny(listOf(ExpandRule, CollectionTypeRule, TypeLambdaRule, TypeLambdaInvocationRule, StructTypeRule, LambdaTypeRule, TupleTypeRule, TypeIdentifierRule.Naked))
			as? TypeExpressionNode
			?: return ParseRule.Result.Failure.Rewind(collector.getCollectedTokens())

		return +node
	}
}

enum class TypeIdentifierRule(private val ctxt: Context = Context.RValue) : ValueRule<TypeIdentifierNode> {
	LValue(Context.LValue), RValue(Context.RValue), Naked(Context.Naked);

	sealed class Errors {
		data class NakedTypeContext(override val sourcePosition: SourcePosition)
			: ParseError("Cannot declare type parameters on a type in this context", sourcePosition)
	}

	enum class Context {
		/// Types in LValue context are allowed to have any kind of type parameters
		LValue,
		/// Type parameters declared on RValue Types must be concrete
		RValue,
		/// In this context, Naked means we expect the type to have no type parameters
		/*
			EXAMPLE: `B` in `type A<B C>`
		 	EXAMPLE: `B` in `type A<B<C>: D>`
		 */
		Naked
	}

	override fun parse(context: Parser) : ParseRule.Result {
		val start = context.peek()

		if (start.type != TokenTypes.TypeIdentifier) {
			return ParseRule.Result.Failure.Rewind()
		}

		val typeId = context.expect(TokenTypes.TypeIdentifier)

		return +TypeIdentifierNode(start, start, typeId.text)
	}
}
