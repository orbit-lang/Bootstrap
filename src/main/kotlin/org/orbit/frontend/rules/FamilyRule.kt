package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.FamilyNode
import org.orbit.core.nodes.TypeDefNode
import org.orbit.core.nodes.TypeIdentifierNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object FamilyRule : ParseRule<FamilyNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Family)
        val id = context.attempt(TypeIdentifierRule.Naked)
            ?: throw invocation.make<Parser>("Expected Type Identifier after `family`", context.peek())

        var next = context.peek()

        if (next.type == TokenTypes.Of) {
            context.consume()

            next = context.peek()
            val members = mutableListOf<TypeDefNode>()
            while (next.type == TokenTypes.TypeIdentifier) {
                val member = context.attempt(TypeIdentifierRule.Naked)
                    ?: return ParseRule.Result.Failure.Abort

                members.add(TypeDefNode(member.firstToken, member.lastToken, member))

                next = context.peek()

                if (next.type == TokenTypes.Comma) {
                    context.consume()
                    next = context.peek()
                }
            }

            return +FamilyNode(id, members, emptyList(), start, next)
        }

        context.expect(TokenTypes.LBrace)

        next = context.peek()

        val members = mutableListOf<TypeDefNode>()
        while (next.type != TokenTypes.RBrace) {
            val member = context.attempt(TypeDefRule)
                ?: throw invocation.make<Parser>("Expected Type in body of `family`", next)

            members.add(member)

            next = context.peek()
        }

        val end = context.expect(TokenTypes.RBrace)

        return +FamilyNode(id, members, emptyList(), start, end)
    }
}