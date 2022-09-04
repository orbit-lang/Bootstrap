package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.AlgebraicConstructorNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object AlgebraicConstructorRule : ParseRule<AlgebraicConstructorNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Constructor)
        val typeIdentifier = context.attempt(TypeIdentifierRule.Naked)
            ?: return ParseRule.Result.Failure.Throw("Expected Type Identifier after `constructor ...`", start)

        if (!context.hasMore) return +AlgebraicConstructorNode(start, typeIdentifier.lastToken, typeIdentifier, emptyList())

        val next = context.peek()

        if (next.type != TokenTypes.LParen) return +AlgebraicConstructorNode(start, typeIdentifier.lastToken, typeIdentifier, emptyList())

        val delim = context.attempt(PairRule.toDelimitedRule())
            ?: return ParseRule.Result.Failure.Throw("Expected list of property pairs after `constructor ${typeIdentifier.value} (`", next)

        return +AlgebraicConstructorNode(start, typeIdentifier.lastToken, typeIdentifier, delim.nodes)
    }
}