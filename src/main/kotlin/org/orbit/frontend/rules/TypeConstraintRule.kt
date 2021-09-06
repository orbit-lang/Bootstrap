package org.orbit.frontend.rules

import org.orbit.core.nodes.TypeConstraintNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TypeConstraintRule : ParseRule<TypeConstraintNode> {
	override fun parse(context: Parser): ParseRule.Result {
		val constrainedTypeIdentifier = context.attempt(TypeIdentifierRule.Naked)
			?: return ParseRule.Result.Failure.Abort

		context.expect(TokenTypes.Colon)

		val constraintTypeIdentifier = context.attempt(TypeExpressionRule)
			?: return ParseRule.Result.Failure.Abort

		return +TypeConstraintNode(
            constrainedTypeIdentifier.firstToken,
            constraintTypeIdentifier.lastToken,
            constrainedTypeIdentifier,
            constraintTypeIdentifier
        )
	}
}