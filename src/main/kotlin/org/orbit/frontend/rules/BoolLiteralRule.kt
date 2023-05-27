package org.orbit.frontend.rules

import org.orbit.core.components.TokenTypes
import org.orbit.core.nodes.BoolLiteralNode
import org.orbit.frontend.extensions.unaryPlus
import org.orbit.frontend.phase.Parser

//object BoolLiteralRule : ValueRule<BoolLiteralNode> {
//    override fun parse(context: Parser): ParseRule.Result {
//        val start = context.expectAny(TokenTypes.True, TokenTypes.False, consumes = true)
//
//        return +BoolLiteralNode(start, start, start.text.toBooleanStrict())
//    }
//}