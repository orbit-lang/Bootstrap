package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ParameterNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ParameterRule : ParseRule<ParameterNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val identifier = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Abort

        val typeIdentifier = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        val next = context.peek()

        if (next.type != TokenTypes.Assignment) return +ParameterNode(
            identifier.firstToken,
            typeIdentifier.lastToken,
            identifier,
            typeIdentifier,
            null
        )

        context.expect(TokenTypes.Assignment)

        val defaultValue = context.attempt(ExpressionRule.defaultValue)
            ?: return ParseRule.Result.Failure.Abort

        return +ParameterNode(identifier.firstToken, typeIdentifier.lastToken, identifier, typeIdentifier, defaultValue)
    }
}