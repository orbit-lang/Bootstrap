package org.orbit.frontend.rules

import org.orbit.core.nodes.TraitConformanceTypeConstraintNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TypeConstraintRule : ParseRule<TraitConformanceTypeConstraintNode> {
	override fun parse(context: Parser): ParseRule.Result {
		val constrainedTypeIdentifier = context.attempt(TypeIdentifierRule.Naked)
			?: return ParseRule.Result.Failure.Abort

		context.expect(TokenTypes.Colon)

		val constraintTypeIdentifier = context.attempt(TypeExpressionRule)
			?: return ParseRule.Result.Failure.Abort

		return +TraitConformanceTypeConstraintNode(
            constrainedTypeIdentifier.firstToken,
            constraintTypeIdentifier.lastToken,
            constrainedTypeIdentifier,
            constraintTypeIdentifier
        )
	}
}