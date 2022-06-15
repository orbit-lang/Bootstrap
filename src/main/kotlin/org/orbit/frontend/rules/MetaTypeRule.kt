package org.orbit.frontend.rules

import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object MetaTypeRule : ValueRule<MetaTypeNode> {
    override fun parse(context: Parser): ParseRule.Result {
        return context.record { recordedTokens ->
            val typeConstructorIdentifier = context.attempt(TypeIdentifierRule.Naked)
                ?: return@record ParseRule.Result.Failure.Abort

            var next = context.peek()

            if (next.type != TokenTypes.LAngle)
                return@record ParseRule.Result.Failure.Rewind(recordedTokens)

            context.consume()

            val typeParameters = mutableListOf<TypeExpressionNode>()

            if (context.peek().type != TokenTypes.RAngle) {
                while (next.type != TokenTypes.RAngle) {
                    // TODO - Allow for recursive meta type parameters here?
                    val typeParameter = context.attempt(TypeExpressionRule)
                        ?: return@record ParseRule.Result.Failure.Rewind(recordedTokens)

                    typeParameters.add(typeParameter)

                    next = context.peek()

                    if (next.type == TokenTypes.Comma) {
                        context.consume()
                        next = context.peek()
                    }
                }
            }

            val end = context.expect(TokenTypes.RAngle)

            return@record +MetaTypeNode(typeConstructorIdentifier.firstToken, end, typeConstructorIdentifier, typeParameters)
        }
    }
}