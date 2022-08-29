package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.BoxNode
import org.orbit.precess.frontend.components.nodes.EntityLookupNode
import org.orbit.precess.frontend.components.nodes.LookupNode

object EntityLookupRule : ParseRule<EntityLookupNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val ctx = context.attempt(ContextLiteralRule)
            ?: return ParseRule.Result.Failure.Abort

        context.expect(TokenTypes.Dot)

        val typeId = context.expect(TokenTypes.TypeId)

        return +EntityLookupNode(ctx.firstToken, typeId, ctx, typeId.text)
    }
}

object BoxRule : ParseRule<BoxNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val start = context.expect(TokenTypes.Box)
        val term = context.attemptAny(listOf(RefLiteralRule, EntityLookupRule))
            as? LookupNode<*>
            ?: return ParseRule.Result.Failure.Abort

        return +BoxNode(start, term.lastToken, term)
    }
}