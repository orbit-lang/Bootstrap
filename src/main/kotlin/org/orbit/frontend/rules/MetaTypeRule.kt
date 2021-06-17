package org.orbit.frontend.rules

import org.orbit.core.nodes.MetaTypeNode
import org.orbit.core.nodes.TypeExpressionNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object MetaTypeRule : ValueRule<MetaTypeNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val typeConstructorIdentifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Abort

        var next = context.peek()

        if (next.type != TokenTypes.LParen)
            return ParseRule.Result.Failure.Rewind(listOf(typeConstructorIdentifier.firstToken))

        context.consume()

        val typeParameters = mutableListOf<TypeExpressionNode>()

        if (context.peek().type != TokenTypes.RParen) {

            while (next.type != TokenTypes.RParen) {
                // TODO - Allow for recursive meta type parameters here?
                val typeParameter = context.attempt(TypeExpressionRule)
                    ?: TODO("")

                typeParameters.add(typeParameter)

                next = context.peek()

                if (next.type == TokenTypes.Comma) {
                    context.consume()
                    next = context.peek()
                }
            }
        }

        val end = context.expect(TokenTypes.RParen)

        return +MetaTypeNode(typeConstructorIdentifier.firstToken, end, typeConstructorIdentifier, typeParameters)
    }
}