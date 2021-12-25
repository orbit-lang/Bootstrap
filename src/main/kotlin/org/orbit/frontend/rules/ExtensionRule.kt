package org.orbit.frontend.rules

import org.orbit.core.nodes.ExtensionNode
import org.orbit.core.nodes.MethodDefNode
import org.orbit.frontend.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

object ExtensionRule : ParseRule<ExtensionNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Extension)
        val targetType = context.attempt(TypeExpressionRule)
            ?: TODO("@ExtensionRule:11")

        val body = context.attempt(BlockRule(MethodDefRule))
            ?: TODO("@ExtensionRule:14")

        return +ExtensionNode(start, body.lastToken, targetType, body.body as List<MethodDefNode>)
    }
}