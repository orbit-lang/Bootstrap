package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.TypeProjectionNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object TypeProjectionRule : ParseRule<TypeProjectionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Type)
        val next = context.peek()

        if (next.type != TokenTypes.Projection) return ParseRule.Result.Failure.Rewind(listOf(start))

        context.consume()

        val typeIdentifier = context.attempt(TypeIdentifierRule.Naked)
            ?: throw invocation.make<Parser>("Expected type identifier after `type projection`", context.peek())

        context.expect(TokenTypes.Colon)

        val traitIdentifierRule = context.attempt(TypeIdentifierRule.Naked)
            ?: throw invocation.make<Parser>("Expected trait identifier after `type projection ${typeIdentifier.value} :`", context.peek())

        // TODO - Body (properties & methods)

        return +TypeProjectionNode(typeIdentifier, traitIdentifierRule, start, traitIdentifierRule.lastToken)
    }
}