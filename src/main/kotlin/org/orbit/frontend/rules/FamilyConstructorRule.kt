package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object FamilyConstructorRule : ParseRule<FamilyConstructorNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Family)
        var next = context.peek()

        if (next.type != TokenTypes.Constructor) return ParseRule.Result.Failure.Rewind(listOf(start))

        context.consume()

        val typeIdentifier = context.attempt(TypeIdentifierRule.Naked)
            ?: throw invocation.make<Parser>("Expected type name identifier after `family constructor`", context.peek())

        next = context.peek()

        if (next.type != TokenTypes.LAngle) {
            // TODO - Parse sum types. Type constructors without type params are allowed,
            //  but you must have at least 1 case constructor, otherwise it doesn't do anything!
            context.forceThrow = true
            throw invocation.make<Parser>(
                "Expected type parameter list after `family constructor ${typeIdentifier.value}`",
                next
            )
        }

        context.consume()
        next = context.peek()

        val typeParameters = mutableListOf<TypeIdentifierNode>()
        while (next.type != TokenTypes.RAngle) {
            val typeParameter = context.attempt(TypeIdentifierRule.Naked)
                ?: throw invocation.make<Parser>("", context.peek())

            typeParameters.add(typeParameter)

            next = context.peek()

            if (next.type == TokenTypes.Comma) {
                context.consume()
                next = context.peek()
            }
        }

        var end = context.expect(TokenTypes.RAngle)

        next = context.peek()

        val whereClauses = mutableListOf<TypeConstraintWhereClauseNode>()
        while (next.type == TokenTypes.Where) {
            val whereClause = context.attempt(TypeConstraintWhereClauseRule)
                ?: return ParseRule.Result.Failure.Abort

            whereClauses.add(whereClause)

            next = context.peek()
            end = whereClause.lastToken
        }

        context.expect(TokenTypes.LBrace)
        next = context.peek()

        val members = mutableListOf<TypeConstructorNode>()
        while (next.type != TokenTypes.RBrace) {
            val member = context.attemptAny(TypeConstructorRule, TypeDefRule())
                ?: throw invocation.make<Parser>("Expected Type in body of `family constructor`", next)

            val typeConstructorNode = when (member.first) {
                null -> member.second!!.promote(typeParameters)
                else -> member.first!!.extend(typeParameters) as TypeConstructorNode
            }

            members.add(typeConstructorNode)

            next = context.peek()
        }

        context.expect(TokenTypes.RBrace)

        return +FamilyConstructorNode(start, end, typeIdentifier, typeParameters, emptyList(), whereClauses, emptyList(), members)
    }
}