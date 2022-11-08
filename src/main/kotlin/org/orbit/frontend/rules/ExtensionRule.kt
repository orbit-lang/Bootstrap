package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.ExtensionNode
import org.orbit.core.nodes.IExtensionDeclarationNode
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.WithNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

private object AnyExtensionDeclarationRule : ParseRule<IExtensionDeclarationNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.peek()
        val node = context.attemptAny(listOf(MethodDefRule, ProjectionRule))
            as? IExtensionDeclarationNode
            ?: return ParseRule.Result.Failure.Throw("Only the following declarations are allowed in the body of an Extension:\n\tMethod, Projection", start)

        return +node
    }
}

object ExtensionRule : ParseRule<ExtensionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Extension)
        val targetType = context.attempt(TypeExpressionRule)
            ?: throw invocation.make<Parser>("Expected Type Identifier after `extension`", context.peek())

        var next = context.peek()

        val contextNode = when (next.type) {
            TokenTypes.Within -> context.attempt(ContextExpressionRule) ?: return ParseRule.Result.Failure.Abort
            else -> null
        }

        next = context.peek()

        if (next.type == TokenTypes.With) {
            val withRule = WithRule(ProjectionRule)
            val withNode = context.attempt(withRule)
                ?: return ParseRule.Result.Failure.Throw("Only the following declarations are allowed in Extension With statements:\n\tProjection", next)

            return +ExtensionNode(start, withNode.lastToken, targetType, listOf(withNode.statement as IExtensionDeclarationNode), contextNode)
        }

        val body = context.attempt(AnyExtensionDeclarationRule.toBlockRule())
            ?: return ParseRule.Result.Failure.Throw("Expected Extension body after `extension ${targetType.value}`", next)

        return +ExtensionNode(start, body.lastToken, targetType, body.body as List<IExtensionDeclarationNode>, contextNode)
    }
}