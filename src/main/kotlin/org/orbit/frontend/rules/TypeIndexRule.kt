package org.orbit.frontend.rules

import org.orbit.core.nodes.TypeIndexNode
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TypeIndexRule : ParseRule<TypeIndexNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.TypeIdentifier)

        if (start.text != "Self") return ParseRule.Result.Failure.Rewind(listOf(start))

        val next = context.peek()

        if (next.type != TokenTypes.LBracket) return ParseRule.Result.Failure.Rewind(listOf(start))

        context.expect(TokenTypes.LBracket)

        val index = context.attempt(TypeIdentifierRule.Naked)
            ?: TODO("@TypeIndexRule:16")

        context.expect(TokenTypes.RBracket)

        return +TypeIndexNode(start, index.lastToken, index)
    }
}