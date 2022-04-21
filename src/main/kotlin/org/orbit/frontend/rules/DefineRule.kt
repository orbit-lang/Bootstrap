package org.orbit.frontend.rules

import org.orbit.core.nodes.DefineNode
import org.orbit.frontend.phase.Parser
import org.orbit.core.components.TokenTypes
import org.orbit.frontend.extensions.unaryPlus

object DefineRule : ParseRule<DefineNode> {
    override fun parse(context: Parser) : ParseRule.Result {
        val start = context.expect(TokenTypes.Define)
        val keySymbol = context.attempt(SymbolRule)!!

        context.expect(TokenTypes.Assignment)

        val outSymbol = context.attempt(SymbolRule)!!

        return +DefineNode(start, outSymbol.lastToken, keySymbol, outSymbol)
    }
}