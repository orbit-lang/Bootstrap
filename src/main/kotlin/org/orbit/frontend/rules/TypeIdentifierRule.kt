package org.orbit.frontend.rules

import org.orbit.core.SourcePosition
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.core.nodes.TypeParametersNode
import org.orbit.frontend.components.ParseError
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.extensions.unaryPlus

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

		val next = context.peek()

		if (ctxt == Context.Naked) {
			// Declaring type parameters on a Type in a naked context is an error
			if (next.type == TokenTypes.LAngle) {
				throw context.invocation.make(TypeIdentifierRule.Errors.NakedTypeContext(next.position))
			}
		}

		if (ctxt == Context.RValue || next.type != TokenTypes.LAngle) {
			// If `Type` is in an rval context, it mustn't consume trailing type parameters
			return +TypeIdentifierNode(start, start, start.text)
		}

		val typeParametersNode = TypeParametersRule(false)
			.execute(context)
			.asSuccessOrNull<TypeParametersNode>()
			?.node
			?: return ParseRule.Result.Failure.Abort

		return +TypeIdentifierNode(start, typeParametersNode.lastToken, start.text, typeParametersNode)
	}
}