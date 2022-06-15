package org.orbit.frontend.rules

import jdk.nashorn.internal.parser.TokenType
import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.IdentifierNode
import org.orbit.frontend.extensions.unaryPlus

object MethodReferenceRule : ValueRule<MethodReferenceNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.mark()
        val start = context.expectOrNull(TokenTypes.TypeIdentifier)
            ?: return ParseRule.Result.Failure.Rewind()
        val recorded = context.end()

        val typeIdentifier = TypeIdentifierNode(start, start, start.text)
        val next = context.peek()

        if (next.type != TokenTypes.Colon) ParseRule.Result.Failure.Rewind(recorded)

        context.expect(TokenTypes.Colon)
        context.expect(TokenTypes.Colon)

        val identifier = context.attempt(IdentifierRule)
            ?: throw context.invocation.make<Parser>("Expected identifier", context.peek().position)

        return +MethodReferenceNode(start, identifier.lastToken, false, typeIdentifier, identifier)
    }
}

object ConstructorReferenceRule : ValueRule<MethodReferenceNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.peek()
        val next = context.peek(1)

        if (start.type != TokenTypes.Colon || next.type != TokenTypes.Colon) return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.Colon)
        context.expect(TokenTypes.Colon)

        val typeExpression = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Abort

        return +MethodReferenceNode(start, typeExpression.lastToken, true, typeExpression, IdentifierNode.init)
    }
}

object InvokableReferenceRule : ValueRule<MethodReferenceNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val methodRef = context.attemptAny(listOf(ConstructorReferenceRule, MethodReferenceRule))
            ?: return ParseRule.Result.Failure.Abort

        return +methodRef
    }
}