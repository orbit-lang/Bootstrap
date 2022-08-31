package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.IProjectionDeclarationNode
import org.orbit.core.nodes.ProjectionNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.main.Parse
import org.orbit.util.Invocation

private object AnyProjectionDeclarationRule : ParseRule<IProjectionDeclarationNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.peek()
        val node = context.attemptAny(listOf(MethodDefRule))
            as? IProjectionDeclarationNode
            ?: return ParseRule.Result.Failure.Throw("Only the following declarations are allowed in the body of a Projection:\n\tMethod", start)

        return +node
    }
}

object ProjectionRule : ParseRule<ProjectionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Projection)

        val typeIdentifier = context.attempt(TypeExpressionRule)
            ?: throw invocation.make<Parser>("Expected type identifier after `type projection`", context.peek())

        var next = context.peek()

        val selfBinding = when (next.type) {
            TokenTypes.LParen -> {
                // Parse identifier for `self` binding
                context.consume()
                val id = context.attempt(IdentifierRule)
                    ?: return ParseRule.Result.Failure.Abort

                context.expect(TokenTypes.RParen)

                id
            }

            else -> null
        }

        context.expect(TokenTypes.Colon)

        val traitIdentifierRule = context.attempt(TypeExpressionRule)
            ?: throw invocation.make<Parser>("Expected trait identifier after `type projection ${typeIdentifier.value} :`", context.peek())

        next = context.peek()
        val whereClauses = mutableListOf<WhereClauseNode>()
        while (next.type == TokenTypes.Where) {
            val whereClause = context.attempt(WhereClauseRule.typeProjection)
                ?: throw invocation.make<Parser>("Expected where clause", context.peek())

            whereClauses += whereClause
            next = context.peek()
        }

        next = context.peek()

        // TODO - Body (properties & methods)
        val body = context.attempt(AnyProjectionDeclarationRule.toBlockRule())
            ?: return ParseRule.Result.Failure.Throw("Expected Projection body after `projection ${typeIdentifier.value}` : ${traitIdentifierRule.value}", next)

        return +ProjectionNode(start, traitIdentifierRule.lastToken, typeIdentifier, traitIdentifierRule, whereClauses, selfBinding, body.body as List<IProjectionDeclarationNode>)
    }
}