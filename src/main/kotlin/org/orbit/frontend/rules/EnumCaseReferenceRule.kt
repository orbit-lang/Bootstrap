package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.EnumCaseReferenceNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object EnumCaseReferenceRule : ValueRule<EnumCaseReferenceNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val collector = context.startCollecting()
        val start = context.expect(TokenTypes.Dot)
        val identifier = context.attempt(IdentifierRule)
            ?: return ParseRule.Result.Failure.Rewind(collector)

        return +EnumCaseReferenceNode(start, identifier.lastToken, identifier)
    }
}