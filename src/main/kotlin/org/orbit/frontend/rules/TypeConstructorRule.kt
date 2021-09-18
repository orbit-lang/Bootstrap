package org.orbit.frontend.rules

import kotlinx.coroutines.yield
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.*
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object EntityConstructorWhereClauseRule : ParseRule<EntityConstructorWhereClauseNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Where)
        val statementNode = context.attemptAny(TypeConstraintRule)
            as? EntityConstructorWhereClauseStatementNode
            ?: return ParseRule.Result.Failure.Abort

        return +EntityConstructorWhereClauseNode(start, statementNode.lastToken, statementNode)
    }
}

object TypeConstructorRule : ParseRule<EntityConstructorNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser) : ParseRule.Result {
        val start = context.expect(TokenTypes.Type)
        var next = context.peek()

        if (next.type != TokenTypes.Constructor) return ParseRule.Result.Failure.Rewind(listOf(start))

        context.consume()

        val typeIdentifier = context.attempt(TypeIdentifierRule.Naked)
            ?: throw invocation.make<Parser>("Expected type name identifier after `type constructor`", context.peek())

        next = context.peek()

        if (next.type != TokenTypes.LAngle) {
            // TODO - Parse sum types. Type constructors without type params are allowed,
            //  but you must have at least 1 case constructor, otherwise it doesn't do anything!
            throw invocation.make<Parser>("Expected type parameter list after `type constructor ${typeIdentifier.value}`", next)
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

        val end = context.expect(TokenTypes.RAngle)

        next = context.peek()

        if (next.type != TokenTypes.LParen) return +TypeConstructorNode(start, end, typeIdentifier, typeParameters)

        val propertiesRule = DelimitedRule(TokenTypes.LParen, TokenTypes.RParen, PairRule)
        val delimitedNode = context.attempt(propertiesRule)
            ?: throw invocation.make<Parser>("Expected property list after type constructor", next)

        val properties = delimitedNode.nodes

        if (properties.isEmpty()) {
            // TODO - output source code in this warning
            invocation.warn("Redundant empty property list", delimitedNode.lastToken)
        }

        next = context.peek()

        val whereClauses = mutableListOf<EntityConstructorWhereClauseNode>()
        while (next.type == TokenTypes.Where) {
            val whereClause = context.attempt(EntityConstructorWhereClauseRule)
                ?: return ParseRule.Result.Failure.Abort

            whereClauses.add(whereClause)

            next = context.peek()
        }

        return +TypeConstructorNode(start, delimitedNode.lastToken, typeIdentifier, typeParameters, properties, whereClauses)
    }
}