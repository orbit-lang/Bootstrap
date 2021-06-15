package org.orbit.frontend.rules

import org.orbit.core.nodes.TypeAliasNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object TypeAliasRule : ParseRule<TypeAliasNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Type)
        val sourceToken = context.expect(TokenTypes.TypeIdentifier)

        // NOTE - We must rewind if there is no "=" token to allow TypeDefRule to pick this up
        //  We also have to ensure TypeAliasRule is attempted BEFORE TypeDefRule to ensure we
        //  don't introduce ambiguities to the grammar
        context.expectOrNull(TokenTypes.Assignment)
            ?: return ParseRule.Result.Failure.Rewind(listOf(start, sourceToken))

        val targetToken = context.expect(TokenTypes.TypeIdentifier)

        return +TypeAliasNode(start, targetToken,
            TypeIdentifierNode(sourceToken, sourceToken, sourceToken.text),
            TypeIdentifierNode(targetToken, targetToken, targetToken.text))
    }
}