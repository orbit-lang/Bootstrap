package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.nodes.DeclNode

object AnyDeclRule : ParseRule<DeclNode<*>> {
    override fun parse(context: Parser): ParseRule.Result {
        val decl = context.attemptAny(listOf(TypeAliasRule, TypeLiteralRule, BindingLiteralRule, SummonValueRule))
            as? DeclNode<*>
            ?: return ParseRule.Result.Failure.Abort

        return +decl
    }
}