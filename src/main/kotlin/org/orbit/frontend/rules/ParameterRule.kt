package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ParameterNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

class ParameterRule(private val allowUntyped: Boolean = false) : ParseRule<ParameterNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val identifier = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Abort

        val typeIdentifier = when (val t = context.attempt(TypeExpressionRule)) {
            null -> when (allowUntyped) {
                true -> TypeIdentifierNode.any()
                else -> return ParseRule.Result.Failure.Abort
            }

            else -> t
        }

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