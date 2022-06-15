package org.orbit.frontend.extensions

import org.orbit.core.nodes.ExpressionNode
import org.orbit.core.nodes.Node
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.rules.ParseRule
//import org.orbit.frontend.rules.PartialCallRule
//import org.orbit.frontend.rules.PartialExpressionRule

//fun <N: Node> ParseRule<N>.parseTrailing(context: Parser, result: ExpressionNode) : ParseRule.Result {
//    val next = context.peek()
//
//    if (TokenTypes.Dot(next)) {
//        val partialCallRule = PartialCallRule(result)
//
//        return partialCallRule.execute(context)
//    } else if (next.type == TokenTypes.OperatorSymbol) {
//        val partialExpressionRule = PartialExpressionRule(result)
//
//        return partialExpressionRule.execute(context)
//    }
//
//    return +result
//}