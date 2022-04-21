package org.orbit.frontend.rules

import org.orbit.core.nodes.DefineNode
import org.orbit.core.nodes.MacroScopeNode
import org.orbit.frontend.phase.Parser
import org.orbit.frontend.extensions.unaryPlus

class MacroScopeRule : ParseRule<MacroScopeNode> {
    override fun parse(context: Parser): ParseRule.Result {
        val defineNodes = mutableListOf<DefineNode>()
        var next = context.peek()

        return +MacroScopeNode(next, next, emptyList())
    }
}