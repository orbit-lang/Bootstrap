package org.orbit.frontend.rules

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.core.nodes.ExtensionNode
import org.orbit.core.nodes.MethodDefNode
import org.orbit.core.nodes.WhereClauseNode
import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.TypeConstructorNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.util.Invocation

object ExtensionRule : ParseRule<ExtensionNode>, KoinComponent {
    private val invocation: Invocation by inject()

    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Extension)
        val targetType = context.attempt(TypeExpressionRule)
            ?: throw invocation.make<Parser>("Expected Type Identifier after `extension`", context.peek())

        val next = context.peek()

        val contextNode = when (next.type) {
            TokenTypes.Within -> context.attempt(ContextExpressionRule) ?: return ParseRule.Result.Failure.Abort
            else -> null
        }

        val body = context.attempt(BlockRule(MethodDefRule))
            ?: throw invocation.make<Parser>("Expected extension body after `extension ${targetType.value}`", context.peek())

        return +ExtensionNode(start, body.lastToken, targetType, body.body as List<MethodDefNode>, contextNode)
    }
}