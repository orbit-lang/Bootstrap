package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.*
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

private object AnyProjectionDeclarationRule : ParseRule<IProjectionDeclarationNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.peek()
        val node = context.attempt(MethodDefRule)
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

        val contextNode = when (next.type) {
            TokenTypes.Within -> context.attempt(ContextExpressionRule) ?: return ParseRule.Result.Failure.Abort
            else -> null
        }

        next = context.peek()

        if (next.type == TokenTypes.With) {
            val resultNode = context.attempt(WithRule(MethodDelegateRule, ProjectedPropertyAssignmentRule))

            val withNode = resultNode as? WithNode<*>
                ?: return ParseRule.Result.Failure.Throw("Only the following declarations are allowed in Projection With statements:\n\tProperty Assignment, Property Delegate, Method Delegate", next)

            return +ProjectionNode(start, traitIdentifierRule.lastToken, typeIdentifier, traitIdentifierRule, emptyList(), selfBinding, listOf(withNode.statement as IProjectionDeclarationNode), contextNode)
        }

        next = context.peek()

        // TODO - Body (properties & methods)
        val body = context.attempt(AnyProjectionDeclarationRule.toBlockRule())
            ?: BlockNode(next, next, emptyList())

        return +ProjectionNode(start, traitIdentifierRule.lastToken, typeIdentifier, traitIdentifierRule, emptyList(), selfBinding, body.body as List<IProjectionDeclarationNode>, contextNode)
    }
}