package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.AttributeDefNode
import org.orbit.core.nodes.TypeEffectNode
import org.orbit.core.nodes.TypeLambdaInvocationNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AttributeDefRule : ParseRule<AttributeDefNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Attribute)
        val identifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Throw("Expected Type Identifier after `attribute`", start)

        context.expect(TokenTypes.Assignment)

        var next = context.peek()
        val expr = context.attempt(AttributeArrowRule)
            ?: return ParseRule.Result.Failure.Throw("Expected Attribute Arrow on right-hand side of Attribute declaration `attribute ${identifier.value}`", next)

        return +AttributeDefNode(start, expr.lastToken, identifier, expr)
    }
}