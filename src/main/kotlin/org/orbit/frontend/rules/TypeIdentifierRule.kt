package org.orbit.frontend.rules

import org.orbit.core.components.SourcePosition
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.core.nodes.TypeParametersNode
import org.orbit.frontend.components.ParseError
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.extensions.unaryPlus

object TypeExpressionRule : ValueRule<TypeExpressionNode> {
	override fun parse(context: Parser): ParseRule.Result {
		val node = context.attemptAny(listOf(MetaTypeRule, TypeIdentifierRule.Naked))
			as? TypeExpressionNode
			?: return ParseRule.Result.Failure.Abort

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
		val start = context.expect(TokenTypes.TypeIdentifier)

		if (!context.hasMore) return +TypeIdentifierNode(start, start, start.text)

		return +TypeIdentifierNode(start, start, start.text)
	}
}