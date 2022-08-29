package org.orbit.precess.frontend.rules

import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
import org.orbit.precess.frontend.components.TokenTypes
import org.orbit.precess.frontend.components.nodes.TypeLookupNode

//object TypeLookupRule : ParseRule<TypeLookupNode> {
//    override fun parse(context: Parser): ParseRule.Result {
//        val type = context.attempt(AnyTypeExpressionRule)
//            ?: return ParseRule.Result.Failure.Abort
//
//        return +TypeLookupNode(type.firstToken, type.lastToken, type)
//    }
//}