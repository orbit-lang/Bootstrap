package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ProjectedPropertyAssignmentNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ProjectedPropertyAssignmentRule : ParseRule<ProjectedPropertyAssignmentNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val identifier = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.Assignment)

        val expression = context.attempt(ExpressionRule.defaultValue)
            ?: return ParseRule.Result.Failure.Abort

        return +ProjectedPropertyAssignmentNode(identifier.firstToken, expression.lastToken, identifier, expression)
    }
}