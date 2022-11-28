package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.IdentifierNode
import org.orbit.core.nodes.MethodReferenceNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object FreeMethodReferenceRule : ValueRule<MethodReferenceNode> {
    override fun parse(context: Parser): ParseRule.Result {
        context.expect(TokenTypes.Colon)
        context.expect(TokenTypes.Colon)

        val next = context.peek()
        val identifier = context.attempt(IdentifierRule)
            ?: throw context.invocation.make<Parser>("Expected identifier after `::` in Free Method Reference expression", next)

        return +MethodReferenceNode(identifier.firstToken, identifier.lastToken, false, TypeIdentifierNode.any(), identifier)
    }
}

object MethodReferenceRule : ValueRule<MethodReferenceNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val start = context.expectOrNull(TokenTypes.TypeIdentifier)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        val typeIdentifier = TypeIdentifierNode(start, start, start.text)

        if (!context.hasMore) return ParseRule.Result.Failure.Rewind(collector)

        val next = context.peek()

        if (next.type != TokenTypes.Colon) ParseRule.Result.Failure.Rewind(collector)

        context.expect(TokenTypes.Colon)
        context.expect(TokenTypes.Colon)

        val identifier = context.attempt(IdentifierRule)
            ?: throw context.invocation.make<Parser>("Expected identifier", context.peek().position)

        return +MethodReferenceNode(start, identifier.lastToken, false, typeIdentifier, identifier)
    }
}

object ConstructorReferenceRule : ValueRule<MethodReferenceNode> {
    override fun parse(context: Parser): ParseRule.Result {
        if (!context.hasAtLeast(2)) return ParseRule.Result.Failure.Abort

        val collector = context.startCollecting()
        val start = context.peek()
        val next = context.peek(1)

        if (start.type != TokenTypes.Colon || next.type != TokenTypes.Colon) return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.Colon)
        context.expect(TokenTypes.Colon)

        val typeExpression = context.attempt(TypeExpressionRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        return +MethodReferenceNode(start, typeExpression.lastToken, true, typeExpression, IdentifierNode.init)
    }
}

object InvokableReferenceRule : ValueRule<MethodReferenceNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val methodRef = context.attemptAny(listOf(ConstructorReferenceRule, MethodReferenceRule, FreeMethodReferenceRule))
            ?: return ParseRule.Result.Failure.Abort

        return +methodRef
    }
}